(module
  (import "c4wa" "printf" (func $printf (param i32) (param i32)))
  (global $__last_offset (mut i32) (i32.const 1053))
  (global $__available_size (mut i32) (i32.const -1))
  (global $@stack (mut i32) (i32.const 8))
  (memory (export "memory") 1)
  (data (i32.const 1024) "2^%d = %ld, as string: '%s'\0A\00")
  (func $long_to_string (param $a i64) (result i32)
    (local $@stack_entry i32)
    (local $N i32)
    (local $buf i32)
    (local $n i32)
    (local $ret i32)
    (local $d i64)
    (set_local $@stack_entry (global.get $@stack))
    (set_local $N (i32.const 20))
    (set_local $buf (global.get $@stack))
    (global.set $@stack (i32.add (i32.const 8) (i32.mul (i32.const 8) (i32.div_s (i32.sub (i32.add (global.get $@stack) (get_local $N)) (i32.const 1)) (i32.const 8)))))
    (set_local $n (get_local $N))
    (loop $@block_1_continue
      (set_local $d (i64.rem_u (get_local $a) (i64.const 10)))
      (set_local $a (i64.div_u (get_local $a) (i64.const 10)))
      (set_local $n (i32.sub (get_local $n) (i32.const 1)))
      (i32.store8 align=1 (i32.add (get_local $buf) (get_local $n)) (i32.add (i32.const 48) (i32.wrap_i64 (get_local $d))))
      (br_if $@block_1_continue (i64.gt_u (get_local $a) (i64.const 0))))
    (set_local $ret (call $malloc (i32.add (i32.sub (get_local $N) (get_local $n)) (i32.const 1))))
    (memory.copy (get_local $ret) (i32.add (get_local $buf) (get_local $n)) (i32.sub (get_local $N) (get_local $n)))
    (i32.store8 align=1 (i32.add (get_local $ret) (i32.sub (get_local $N) (get_local $n))) (i32.const 0))
    (global.set $@stack (get_local $@stack_entry))
    (get_local $ret))
  (func $main (export "main") (result i32)
    (local $@stack_entry i32)
    (local $n i32)
    (local $a i64)
    (set_local $@stack_entry (global.get $@stack))
    (block $@block_1_break
      (set_local $a (i64.const 1))
      (loop $@block_1_continue
        (br_if $@block_1_break (i32.ge_s (get_local $n) (i32.const 64)))
        (i64.store align=8 (global.get $@stack) (i64.extend_i32_s (get_local $n)))
        (global.set $@stack (i32.add (global.get $@stack) (i32.const 8)))
        (i64.store align=8 (global.get $@stack) (get_local $a))
        (global.set $@stack (i32.add (global.get $@stack) (i32.const 8)))
        (i64.store align=8 (global.get $@stack) (i64.extend_i32_s (call $long_to_string (get_local $a))))
        (global.set $@stack (i32.sub (global.get $@stack) (i32.const 16)))
        (call $printf (i32.const 1024) (global.get $@stack))
        (set_local $a (i64.mul (get_local $a) (i64.const 2)))
        (set_local $n (i32.add (get_local $n) (i32.const 1)))
        (br $@block_1_continue)))
    (global.set $@stack (get_local $@stack_entry))
    (i32.const 0))
  (func $malloc (param $size i32) (result i32)
    (local $res i32)
    (local $pages i32)
    (if (i32.lt_s (global.get $__available_size) (i32.const 0))
      (then
        (global.set $__available_size (i32.mul (i32.const 64000) (memory.size)))))
    (global.set $__last_offset (i32.add (i32.mul (i32.div_s (i32.sub (global.get $__last_offset) (i32.const 1)) (i32.const 8)) (i32.const 8)) (i32.const 8)))
    (set_local $res (global.get $__last_offset))
    (global.set $__last_offset (i32.add (global.get $__last_offset) (get_local $size)))
    (if (i32.gt_s (global.get $__last_offset) (global.get $__available_size))
      (then
        (set_local $pages (i32.add (i32.const 1) (i32.div_s (global.get $__last_offset) (i32.const 64000))))
        (drop (memory.grow (i32.sub (get_local $pages) (memory.size))))
        (global.set $__available_size (i32.mul (i32.const 64000) (get_local $pages)))))
    (get_local $res)))
