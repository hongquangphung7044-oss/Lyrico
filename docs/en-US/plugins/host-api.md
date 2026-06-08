# Host API Reference

Plugins access host functionality through `globalThis.Platform`.

```javascript
var text = Platform.http.getText("https://example.com/api")
Platform.log.info("Example", text)
```

## HTTP

Use the HTTP API for network requests.

```javascript
var text = Platform.http.getText("https://example.com/search?q=test")

var json = Platform.http.getJson("https://example.com/search", {
  query: {
    q: "test"
  },
  headers: {
    "User-Agent": "Lyrico Plugin"
  }
})

var response = Platform.http.request({
  method: "POST",
  url: "https://example.com/api",
  headers: {
    "Content-Type": "application/json"
  },
  body: JSON.stringify({ q: "test" })
})
```

Check the current host implementation for the exact request object fields supported by your app version.

## Logging

```javascript
Platform.log.debug("Example", "debug message")
Platform.log.info("Example", "request started")
Platform.log.warn("Example", "fallback used")
Platform.log.error("Example", "request failed")
```

Avoid logging passwords, cookies, tokens, or full authenticated URLs.

## Encoding And Crypto

Host APIs may expose helpers for encoding, hashing, encryption, and compression. Prefer host helpers when a source requires a specific algorithm that is hard to implement safely in plain JavaScript.

```javascript
var encoded = Platform.encoding.base64Encode("hello")
var decoded = Platform.encoding.base64Decode(encoded)
```

## XML

Use the XML API for lightweight XML/TTML lookup and rewriting. The host parses and serializes the document again, so the output preserves elements, text, and attributes, but it is not a byte-for-byte formatter. Attribute prefixes are kept as part of the attribute name, for example `xml:lang`.

### `Platform.xml.getRootAttributes(xml)`

Returns the root element attributes as an object.

```javascript
var attrs = Platform.xml.getRootAttributes(ttml)
var language = attrs["xml:lang"] || attrs.lang || ""
```

### `Platform.xml.findElements(xml, query)`

Recursively finds matching elements. `query.tag` matches the element name. `query.attrs` is an exact-match object, and all listed attributes must match.

```javascript
var translations = Platform.xml.findElements(ttml, {
  tag: "translation",
  attrs: {
    "xml:lang": "zh-Hans"
  }
})
```

Each returned element has this shape:

```json
{
  "tag": "translation",
  "attrs": {
    "xml:lang": "zh-Hans",
    "type": "replacement"
  },
  "text": "plain text",
  "innerXml": "<text for=\"l1\">...</text>",
  "children": []
}
```

### `Platform.xml.replaceChildrenByAttr(xml, options)`

Finds elements by tag, reads a key attribute, and replaces all child nodes when a matching replacement exists. This is useful for applying translated TTML text back to `<p>` nodes by `itunes:key`.

```javascript
var localized = Platform.xml.replaceChildrenByAttr(ttml, {
  targetTag: "p",
  keyAttr: "itunes:key",
  replacements: {
    "l1": {
      mode: "text",
      value: "First line"
    },
    "l2": {
      mode: "xml",
      value: "<span begin=\"1s\">Second line</span>"
    }
  },
  rootAttributes: {
    "xml:lang": "zh-Hans"
  }
})
```

`mode: "text"` writes escaped text. `mode: "xml"` parses `value` as an XML fragment. If `targetTag` or `keyAttr` is empty, the original XML is returned.

### `Platform.xml.removeElements(xml, query)`

Recursively removes matching child elements and returns the rewritten XML. The root element itself is not removed.

```javascript
var cleaned = Platform.xml.removeElements(ttml, {
  tag: "translation",
  attrs: {
    "xml:lang": "zh-Hans",
    type: "replacement"
  }
})
```

## Error Handling

Wrap network calls and parsing in `try` / `catch`, then return an empty result or a structured failure that the host can display.

```javascript
function searchSongs(request) {
  try {
    var data = Platform.http.getJson("https://example.com/search", {
      query: { q: request.keyword || "" }
    })

    return JSON.stringify(data.items || [])
  } catch (err) {
    Platform.log.warn("Example", "search failed: " + err.message)
    return JSON.stringify([])
  }
}
```
