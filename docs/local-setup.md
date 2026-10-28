# Windows development setup

Run from PowerShell in D:\GuestHouse.

Java 21 was found at C:\Program Files\Java\jdk-21. Select it for the current shell:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
java -version
node --version
npm.cmd --version
docker version
```

Node v23.11.0 and npm 11.3.0 were detected. A supported Node LTS version will be selected and pinned when the frontend is scaffolded. Do not assume that an installed Docker executable means the Docker daemon is running.

Create your local environment file before starting services:

```powershell
Copy-Item .env.example .env
```

Replace the example secrets. Application startup commands will be added alongside the implementation and verified before being marked working.
