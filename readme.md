# Emmy Lua Language Server

Emmy lua Language server have lots of features for lua language, including:
* Find usages
* Go to definition
* Comment based type/class annotation
* Basic completion

For an exhaustive list of features see the [intellij plugin description](https://github.com/EmmyLua/IntelliJ-EmmyLua).

## Requirements

* [install JDK](https://www3.ntu.edu.sg/home/ehchua/programming/howto/JDK_Howto.html)

## Building

Run from root:

    $ gradlew shadowJar


The `EmmyLua-LS-all.jar` file will be created in `EmmyLua-LanguageServer/EmmyLua-LS/build` .

## Running Server

To run the language server use:

    $ java -cp EmmyLua-LS-all.jar com.tang.vscode.MainKt`

## Adding to an IDE

Just pass the instantiating instruction to the LSP plugin.

Example: adding EmmyLua to [SublimeText](https://www.sublimetext.com/) with [Sublime-LSP](https://github.com/tomv564/LSP):
* install the `LSP` plugin in sublime
* add emmy as a client to `LSP.sublime-settings`:
```json
{
    "clients":
    {
        "emmy":
        {
            "command":
            [
                "java",
                "-cp",
                "<path to jar>/*",
                "com.tang.vscode.MainKt"
            ],
            "enabled": true,
            "languageId": "lua",
            "scopes":
            [
                "source.lua"
            ],
            "syntaxes":
            [
                "Packages/Lua/Lua.sublime-syntax"
            ]
        }
    }
}
```