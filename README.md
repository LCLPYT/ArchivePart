# ArchivePart
A tool to manage archives that are split into multiple parts e.g. for use of big archives on FAT32.

It does not support compression but a simple encryption method.
Can be used to store archives bigger than 4GB on **android**.

## Requirements
- Java 8+

## Usage
In order to run the program via command line, you have to download a runnable release from the 'releases' tab.
Then execute the file via command line:
`java -jar ArchivePart-cmd.jar --help`

## Implementation for developers
Gradle:

**Step 1.** Add a custom repository to your repositories
```gradle
repositories {
    maven { url 'https://repo.lclpnet.work/repository/internal' }
}
```
**Step 2.** Add the dependency
```gradle
dependencies {
    implementation 'work.lclpnet:archivepart:2.1.0'
}
```

Maven:

**Step 1.** Add a custom repository to your repositories
```xml
<repositories>
    <repository>
      <id>lclpnetwork</id>
      <name>LCLPNetwork Repository</name>
      <url>https://repo.lclpnet.work/repository/internal</url>
    </repository>
</repositories>
```
**Step 2.** Add the dependency
```xml
  <dependency>
    <groupId>work.lclpnet</groupId>
    <artifactId>archivepart</artifactId>
    <version>2.1.0</version>
  </dependency>
```
