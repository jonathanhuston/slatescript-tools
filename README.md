# slatescript-tools

Tools for validating translated Word documents

## Usage

    $ java -jar slatescript-tools-0.1.0-standalone.jar <tool> docx1 [docx2]
    
    Tools:
       txt docx:                converts docx1 to UTF-8 text
       parens docx1:            checks whether parentheses are balanced
       checknums docx1 docx2:   checks whether digits are the same in docx1 and docx2
