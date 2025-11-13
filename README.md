# slatescript-tools

Tools for validating translated Microsoft Word (.docx) documents

## Installation

git clone https://github.com/jonathanhuston/slatescript-tools.git

lein uberjar

or:

docker pull jonathanhuston/slatescript-tools

## Usage

    $ java -jar slatescript-tools-0.1.0-SNAPSHOT-standalone.jar <tool> docx1 [docx2]
    
    or bash helper script for running (and installing) Docker image:
    
    $ sstd <tool> docx1 [docx2]
    
    Tools:
       txt docx1:               converts docx1 to UTF-8 text
       parens docx1:            checks whether parentheses are balanced
       checknums docx1 docx2:   checks whether digits are the same in docx1 and docx2
       validate docx1 docx2:    AI-powered validation of translation completeness (local installation only)

## validate Tool

The `validate` tool uses Claude AI to analyze translations and identify any information from the source document that may have been omitted in the translation.

**Requirements:**
- Local installation only (not available in Docker version)
- Anthropic API key required

**Setup:**

    $ export ANTHROPIC_API_KEY=your_api_key_here

**Usage:**

    $ java -jar slatescript-tools-0.1.0-SNAPSHOT-standalone.jar validate source.docx translated.docx

The tool generates a markdown report saved as `translated-validation.md` (or `translated-validation-1.md`, etc. if the file already exists) in the same directory, listing any omissions found in the translation.
