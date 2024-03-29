const cssId = "css"

htmx.on("#akar", "htmx:oobBeforeSwap", event => {
    if (event.detail.elt.id == "akar") {
        // https://stackoverflow.com/a/2024618/8812880 modified
        var queryString = '?reload=' + new Date().getTime();
        var links = document.getElementsByTagName("link")
        for (const idx in links) {
            if (Object.hasOwnProperty.call(links, idx)
                && links[idx].type === 'text/css'
                && links[idx].hasAttribute('title')) {
                links[idx].href = links[idx].href.replace(/\?.*|$/, queryString)
                console.log("css [" + links[idx].href + "] refreshed!")
            }
        }
    }
});

console.log("cssRefresher registered!")