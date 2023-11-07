# bbangsearch

A CLI for [DuckDuckGo's bang❗ searches](https://duckduckgo.com/bangs) written in [Babashka](https://babashka.org/).

## Installation

### homebrew

TBD

### standalone

TBD

### bbin

[bbin](https://github.com/babashka/bbin) allows for easy installation of Babashka scripts.

#### Prerequisites

[install bbin](https://github.com/babashka/bbin#installation) (make sure to adjust $PATH).

```shell
# latest version from mainline
$ bbin install io.github.eval/bbangsearch
$ bbang -h

# as something else
$ bbin install io.github.eval/bbangsearch --as bangbang
```

## Usage

### ❗

```shell
# search github
$ bbang gh bbangsearch

# open (Google's) search-page
$ bbang g

# just the url
$ bbang gh bbangsearch --url
```

### What ❗?

```shell
# print table
$ bbang bangs:ls

# bangs from specific domain
$ bbang bangs:ls | grep -e '\.dk'

# Using DuckDuckGo's bang search
$ bbang bangs github
```

## License

Copyright (c) 2023 Gert Goet, ThinkCreate. Distributed under the MIT license. See [LICENSE](./LICENSE).
