# Building RankedSMP into a plugin .jar

This folder is the **source code** for the plugin (a Maven project). Paper
cannot run source code directly — it needs to be **compiled** into a `.jar`
that contains real `.class` files. Pick whichever option below is easiest
for you. All three produce the same file: `target/RankedSMP-3.0.jar`.

---

## Option A — GitHub Actions (no installs required)

1. Create a new repository on GitHub (can be private).
2. Upload **everything in this folder** (including the hidden `.github`
   folder) to the repo root — either drag-and-drop on the GitHub web UI,
   or via git:
   ```
   git init
   git add .
   git commit -m "Initial commit"
   git branch -M main
   git remote add origin <your-repo-url>
   git push -u origin main
   ```
3. Go to the **Actions** tab of your repo. A workflow called
   "Build RankedSMP Plugin" will run automatically (takes ~1 minute).
4. Click the completed run, then download the **RankedSMP-plugin-jar**
   artifact under "Artifacts". Unzip it to get `RankedSMP-3.0.jar`.

## Option B — Docker (one command, no Java/Maven install)

If you have Docker installed, run this from inside this folder:

```
docker run --rm -v "$PWD":/app -w /app maven:3.9-eclipse-temurin-21 mvn -B clean package
```

The compiled jar will appear at `target/RankedSMP-3.0.jar`.

## Option C — Local Maven

If you have JDK 21 and Maven installed:

```
mvn clean package
```

The compiled jar will appear at `target/RankedSMP-3.0.jar`.

---

## Installing the plugin

1. Stop your server (or just remove the old plugin first).
2. Delete the broken `RankedSMP-source.jar` from `plugins/`.
3. Copy `RankedSMP-3.0.jar` into `plugins/`.
4. Start the server. You should see the RankedSMP banner in the console
   and no "Cannot find main class" error.
