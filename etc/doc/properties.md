Name | Default | Description
---- | ------- | -----------
module.importName | c4wa | module name used in imported symbols
module.memoryStatus | export:memory | `export:name` export memory as _name_; <br>`import:name` import memory as _name_; <br>`internal` memory is neither exported nor imported; <br>`none` if no memory at all
module.dataOffset | 1024 | Memory offset to start allocating strings with `(data ...)`
module.dataLength | 1024 | Maximum memory (in bytes) to be reserved for strings 