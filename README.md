# panas.reload

A hot reload for serving html (or htmx) with just babashka ('panas' is an Indonesian word for 'hot')

needs babashka version > 1.0.169

## Quick setup

create a `bb.edn` file, specify source path and create a task like below (using latest git sha from the `main` branch of this repo)

for example:

```clojure
{:paths ["src"]
 :tasks {panas.reload {:extra-deps {io.github.keychera/panas.reload {:git/sha "a210d4539472ac462ca6f8194144ba7bb245c1a0"}}
                       :requires ([panas.reload])
                       :task (panas.reload/-main 'your-namespace/your-router {:port 42042})}}}
```

the task will call `panas.reload/-main` that have the same signature as `org.httpkit.server/run-server` plus one optional argument for configuration for panas.reload itself, which are `handler`, `server-opts`, and `server-opts`

1. `handler` - a fully qualified symbol that refers to your ring-router function, e.g `'your-namespace/your-router`
2. `server-opts` - a map that will be passed to httpkit as-is
3. `panas-opts` is a map of the following:
```clojure
{:watch-dir "path/to/dir" ;; this specify the directory to watch file changes, 
                          ;; default to the first classpath root (from the value of `(io/resource "")`)
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
- issues in determining reloadable urls
- currently file watching only one folder
- unicode characters might break on reload (this might be incomplete encoding information issue in http response but is still under investigation)
- planning to make this usable in Clojure JVM, as well as making this work with other server (currently locked)
- css refresh currently select all `link` tag that is `type="text/css"` and have `title` attribute (initial reasoning: so we can differentiate which css to reload)

## How it works

WIP

some notes for later:
- uses htmx and its websocket extentsion
- 
