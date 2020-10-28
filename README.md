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
       txt docx:                converts docx1 to UTF-8 text
       parens docx1:            checks whether parentheses are balanced
       checknums docx1 docx2:   checks whether digits are the same in docx1 and docx2
