# panas.reload
[![bb compatible](https://raw.githubusercontent.com/babashka/babashka/master/logo/badge.svg)](https://babashka.org)

A hot reload for serving html (or htmx) with just babashka ('panas' is an Indonesian word for 'hot')

needs babashka version > 1.0.169

> ⚠️ currently still figuring things how to distribute codes, expect a lot of changes for now

## Quick setup

### with [`bbin`](https://github.com/babashka/bbin)

If you just want live reload for static assets, you can

```sh
bbin install https://raw.githubusercontent.com/keychera/panas.reload/main/bbin/panas.file-server.clj
panas --dir path/to/static/assets
```
note: 
1. `panas.file-server.clj` is an extension of [babashka/http-server](https://github.com/babashka/http-server)
2. live reload will works if the file is a complete html (having `<html>`, `<head>`, and `<body>` tags)
3. css will live reload if its in a link tag with `type='text/css'` and a `title` tag e.g: 
```
<link href="/styles.css" rel="stylesheet" type="text/css" title="default">
```

### as a babashka project

if you want live reload a server that returns html and css:

create a `bb.edn` file, specify source path and create a task like below (using latest git sha from the `main` branch of this repo)

for example:

```clojure
{:paths ["src"]
 :tasks {panas.reload {:extra-deps {io.github.keychera/panas.reload {:git/sha "d80ee6d6dbfa8bab2a52e87f1da0e1682cef167c"}}
                       :requires ([panas.reload] your-namespace)
                       :task (panas.reload/-main your-namespace/your-router {:port 42042})}}}
```

the task will call `panas.reload/-main` that have the same signature as `org.httpkit.server/run-server` plus two optional argument for configuration for panas.reload itself, which are `handler`, `server-opts`, and `server-opts`

1. `handler` - a fully qualified symbol that refers to your ring-router function, e.g `'your-namespace/your-router`
2. `server-opts` - (Optional) a map that will be passed to httpkit as-is
3. `panas-opts` - (Optional) a map of the following config keywords:
```clojure
{:watch-dir "path/to/dir"
 ;; this specify the directory to watch file changes, 
 ;; default to the first classpath root (from the value of `(io/resource "")`)
 :reloadable? predicate-fn 
 ;; this is to override predicate that determine which url to reload, 
 ;; the default is `panas.default/reloadable?`.
 ;; Other than overriding, you can also extend the default using clojure built-in `every-pred` e.g. `(every-pred your-pred panas.default/reloadable?)`
}
``` 

the run the task `panas.reload` with

```sh
bb panas.reload
```

make some changes in any file in your `src`!

## Examples

you can try some examples here: https://github.com/keychera/panas.example

## Some quirks

WIP

some notes for later:
- currently file watching only one folder
- planning to make this usable in Clojure JVM, as well as making this work with other server (current impl locked to httpkit server but personally I am using this same but tweaked code on JVM with ring-jetty and works pretty well)
- css refresh currently select all `link` tag that is `type="text/css"` and have `title` attribute (initial reasoning: so we can differentiate which css to reload)
- htmx websocket inside akar body has issues

## How it works

WIP

some notes for later:
- uses htmx and its websocket extentsion
- 


## Development

WIP
- testing use etaoin + ChromeDriver 111.0.5563.64
