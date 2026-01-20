# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

slatescript-tools is a Clojure-based CLI tool for validating translated Microsoft Word (.docx) documents. The tool provides four main functions:
1. Convert docx to plain text
2. Check if parentheses are balanced
3. Compare numbers between two docx files
4. AI-powered translation completeness validation

## Build & Run Commands

### Local Development (Leiningen)
```bash
# Build uberjar
lein uberjar

# Run the tool directly with Java
java -jar target/uberjar/slatescript-tools-0.1.0-SNAPSHOT-standalone.jar <tool> docx1 [docx2]

# Run tests
lein test
```

### Docker
```bash
# Build Docker image
docker build -t jonathanhuston/slatescript-tools .

# Pull pre-built image
docker pull jonathanhuston/slatescript-tools

# Use the sstd bash helper script (handles Docker automatically)
./sstd <tool> docx1 [docx2]
```

### Tool Commands
```bash
# Convert docx to UTF-8 text file
<tool> txt document.docx

# Check parentheses balance
<tool> parens document.docx

# Compare numbers between source and translated documents
<tool> checknums source.docx translated.docx

# AI-powered validation of translation completeness
<tool> validate source.docx translated.docx
```

### Environment Variables
```bash
# Required for the validate tool
export ANTHROPIC_API_KEY=your_api_key_here
```

## Architecture

### Document Processing Pipeline

The tool follows a multi-stage pipeline for processing .docx files:

1. **Shell Layer** (`shell.clj`): Handles file system operations
   - `create-xml`: Unzips .docx (which is a zip archive) to extract XML files
   - Returns path to `word/document.xml` (the main document body)
   - `remove-xml`: Cleans up extracted folders after processing
   - `create-docx`: Reverse operation (not currently used by main tools)

2. **XML Parsing Layer** (`plain-text.clj`): Converts WordprocessingML to plain text
   - Parses XML using `clojure.xml`
   - Handles Word-specific tags (`:w:p` for paragraphs, `:w:br` for breaks)
   - Filters out formatting tags (`:w:rFonts`, `:w:color`, `:w:sz`, etc.)
   - Normalizes whitespace while preserving document structure

3. **Validation Layers**: Process the plain text output
   - `check-parens.clj`: Detects unbalanced parentheses by tracking consecutive pairs
   - `check-numbers.clj`: Compares digit sequences between two documents
     - Uses `indexed-digits` to track position of every digit
     - Uses `clojure.data/diff` to find mismatches
     - Returns context strings (40 chars) around each mismatch
     - Copies first mismatch context to clipboard automatically
   - `validate.clj`: AI-powered translation completeness analysis
     - Calls Anthropic Claude API to analyze translation omissions
     - Intelligently chunks long documents (>50k chars) for analysis
     - Generates markdown reports identifying missing information
     - Outputs to `<target>-validation.md` file

### Entry Point

`core.clj` serves as the main entry point and orchestrates the pipeline:
- `-main` function handles CLI argument parsing and validation
- `validate-args` checks argument count and file existence
- Each tool function (`txt`, `parens`, `checknums`) composes the pipeline stages
- All functions ensure cleanup with `remove-xml` calls

### Key Design Patterns

**Pipeline Composition**: Tools compose functions using threading macros (`->`, `->>`) to create clear data transformation pipelines.

**Temporary State Management**: `.docx` files are unzipped to temporary folders during processing, then cleaned up. This allows XML parsing without keeping large structures in memory.

**Result Structure**: Validation tools return structured data:
- `check-parens`: `{:open [indexes] :closed [indexes]}`
- `check-numbers`: `[[context1-slices] [context2-slices]]` (max 10 each)

## Project Structure

```
src/slatescript_tools/
├── core.clj           # Main entry point, CLI handling, argument validation
├── shell.clj          # File system operations (unzip/zip docx)
├── plain_text.clj     # XML to plain text conversion
├── check_parens.clj   # Parenthesis balance validation
├── check_numbers.clj  # Digit comparison between documents
├── validate.clj       # AI-powered translation validation
└── clipboard.clj      # macOS clipboard integration
```

## Notable Implementation Details

- **Number Comparison**: The `check-numbers` tool returns up to 10 mismatched contexts and automatically copies the first mismatch to the clipboard for quick review
- **Parenthesis Detection**: Identifies unbalanced parentheses by finding consecutive identical parens (e.g., "((" or "))")
- **Context Slicing**: When reporting mismatches, provides 20 characters before and after the position (configurable via `slice` constant)
- **AI Translation Validation**: Uses Claude Sonnet 4.5 via Anthropic API to identify omissions in translations
  - Automatically chunks documents over 50k characters for analysis
  - Uses structural chunking (matching headings between source/target) when possible
  - Falls back to paragraph-based chunking if < 2 headings match
  - Strips Word formatting prefixes (`left`, `center`, etc.) before heading detection
  - Splits on `\n+` to match actual paragraph structure from plain-text extraction
  - Each chunk analyzed separately with results combined in final report
  - Requires `ANTHROPIC_API_KEY` environment variable
  - Note: validate tool is NOT available via Docker/sstd — run directly with Java
- **Platform Assumption**: Uses bash commands (`unzip`, `rm`, `zip`) and assumes Unix-like environment
- **Java AWT Dependency**: Clipboard functionality uses `java.awt.Toolkit` for system clipboard access

## Building Old Versions for A/B Testing

To build an old version for comparison testing (e.g., after changing the validate tool):

```bash
# Save current commit hash
CURRENT=$(git rev-parse HEAD)

# Checkout old version, build, save jar to project root (not target/ which gets cleaned)
git checkout <old-commit> -- .
lein uberjar
cp target/uberjar/slatescript-tools-0.1.0-SNAPSHOT-standalone.jar ./slatescript-tools-OLD.jar

# Restore current version and rebuild
git checkout $CURRENT -- .
lein uberjar
```

Then compare:
```bash
java -jar slatescript-tools-OLD.jar validate source.docx target.docx
java -jar target/uberjar/slatescript-tools-0.1.0-SNAPSHOT-standalone.jar validate source.docx target.docx
```

**Important**: Copy the old jar to the project root, not `target/uberjar/`, because `lein uberjar` cleans the target directory before building.
