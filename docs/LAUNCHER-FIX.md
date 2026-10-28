# Windows launcher syntax repair

## Symptom

`RUN_ALL.ps1:64` - `The string is missing the terminator: "`
Followed by errors at lines 32 and 70 about missing braces and try/catch.

## Cause

The older `RUN_ALL.ps1` included a Unicode em dash in a double-quoted
`Write-Host` string, but was saved as UTF-8 **without a BOM**.
Windows PowerShell 5.1 reads that source as the system ANSI code page;
when interpreted under Windows-1252, the em dash may produce an extra
curly double-quote character. PowerShell treats curly double quotes as
string delimiters, so the parser fails before any Docker commands run.

## Fix

This revision uses ASCII-only bytes in `scripts/automation/RUN_ALL.ps1`.
No PowerShell policy changes or extra installs are necessary.

## Verify

Extract to a NEW folder, start Docker Desktop, and double-click
`motoservice-lite/RUN_ALL.cmd`. The previous script did not run *any*
tests on your computer. This fix removes only the parser error; database,
application, and integration-test pass status still depends on Docker
and real execution on the user's machine.
