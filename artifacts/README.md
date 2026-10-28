# Local verification output

This folder initially contains **no passed runtime evidence**.

After a successful `RUN_ALL.cmd` execution on a Windows host with Docker Desktop:

- `full-verification-YYYYMMDD-HHMMSS.log`: full launcher transcript.
- `http-smoke.json`: genuine registered-motorcycle → booking → waitlist → promotion → advisor completion result, written only after success.
- `evaluation-results.md`: copy of the genuine test-database synthetic evaluation.
- `LOCAL_VERIFICATION.txt`: generated only after Maven verification, app startup and HTTP smoke all exit successfully.

Detailed JUnit and experiment outputs are in `target/`, which is ignored by Git. An unexecuted script cannot produce verified results. These files are not present in the distributed source package.
