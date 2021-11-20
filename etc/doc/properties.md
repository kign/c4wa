Name | Default | Description
---- | ------- | -----------
module.importName | c4wa | module name used in imported symbols
module.memoryStatus | export:memory | `export:name` export memory as _name_; <br>`import:name` import memory as _name_; <br>`internal` memory is neither exported nor imported; <br>`none` if no memory at all
module.stackSize | 1024 | Size of main stack in bytes
module.dataSize | 1024 | Size of data segment allocated for strings, in bytes 