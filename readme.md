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

    $ java -cp EmmyLua-LS-all.jar com.tang.vscode.MainKt

## Adding to an Sublime

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

## Adding to Emacs
add following code to your `~/.emacs` or `.emacs.d/init.el` .
``` emacs-lisp
(use-package lsp-mode
  :ensure t
  :commands lsp
  :hook ((lua-mode) . lsp)
  :config

  ;; register emmy-lua-lsp
  (lsp-register-client
   (make-lsp-client :new-connection
                    (lsp-stdio-connection
                     (list
                      "/usr/bin/java"
                      "-cp"
                      (expand-file-name "EmmyLua-LS-all.jar" user-emacs-directory)
                      "com.tang.vscode.MainKt"))
                    :major-modes '(lua-mode)
                    :server-id 'emmy-lua))
  )

(use-package company-lsp
  :ensure t
  :after lsp-mode
  :config
  (setq company-lsp-enable-recompletion t)
  )

(defun company-lua-mode-setup()
  "Create lua company backend."
  (setq-local company-backends '(
                                 (
                                  company-lsp
                                  company-lua
                                  company-keywords
                                  company-gtags
                                  company-yasnippet
                                  )
                                 company-capf
                                 company-dabbrev-code
                                 company-files
                                 )
       ))

(use-package lua-mode
  :ensure t
  :mode "\\.lua$"
  :interpreter "lua"
  :hook (lua-mode . company-lua-mode-setup)
  :config
  )

```
work with company-mode and lua-mode in Emacs 26.1:

![work-with-emacs](img/work-with-emacs.png)
