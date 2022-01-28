(module
  (import "c4wa" "printf" (func $printf (param i32) (param i32)))
  (global $@stack (mut i32) (i32.const 8))
  (memory (export "memory") 1)
  (data (i32.const 1024) "Count = %d\0A\00")
  (func $main (export "main") (result i32)
    (local $@stack_entry i32)
    (local $count i32)
    (local $x i32)
    (local $y i32)
    (set_local $@stack_entry (global.get $@stack))
    (block $@block_1_break
      (loop $@block_1_continue
        (br_if $@block_1_break (i32.ge_s (get_local $x) (i32.const 5)))
        (block $@block_1_1_break
          (set_local $y (i32.const 0))
          (loop $@block_1_1_continue
            (br_if $@block_1_1_break (i32.ge_s (get_local $y) (i32.const 5)))
            (set_local $count (i32.add (get_local $count) (i32.const 1)))
            (set_local $y (i32.add (get_local $y) (i32.const 1)))
            (br $@block_1_1_continue)))
        (set_local $x (i32.add (get_local $x) (i32.const 1)))
        (br $@block_1_continue)))
    (i64.store align=8 (global.get $@stack) (i64.extend_i32_s (get_local $count)))
    (call $printf (i32.const 1024) (global.get $@stack))
    (global.set $@stack (get_local $@stack_entry))
    (i32.const 0)))
