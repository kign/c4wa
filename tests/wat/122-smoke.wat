(module
  (import "c4wa" "printf" (func $printf (param i32) (param i32)))
  (global $d (mut i32) (i32.const 3))
  (global $@stack (mut i32) (i32.const 0))
  (memory (export "memory") 1)
  (data (i32.const 1024) "x   x%%%d  floor ceil\0A=====================\0A\00%2d  %2d   %2d    %2d\0A\00")
  (func $mod_ceiling (param $x i32) (result i32)
    (select (get_local $x) (select (i32.sub (i32.add (get_local $x) (global.get $d)) (i32.rem_s (get_local $x) (global.get $d))) (i32.sub (get_local $x) (i32.rem_s (get_local $x) (global.get $d))) (i32.gt_s (get_local $x) (i32.const 0))) (i32.eqz (i32.rem_s (get_local $x) (global.get $d)))))
  (func $mod_floor (param $x i32) (result i32)
    (select (get_local $x) (select (i32.sub (get_local $x) (i32.rem_s (get_local $x) (global.get $d))) (i32.sub (i32.sub (get_local $x) (global.get $d)) (i32.rem_s (get_local $x) (global.get $d))) (i32.gt_s (get_local $x) (i32.const 0))) (i32.eqz (i32.rem_s (get_local $x) (global.get $d)))))
  (func $main (export "main") (result i32)
    (local $@stack_entry i32)
    (local $i i32)
    (set_local $@stack_entry (global.get $@stack))
    (i64.store (global.get $@stack) (i64.const 1024))
    (global.set $@stack (i32.add (global.get $@stack) (i32.const 8)))
    (i64.store (global.get $@stack) (i64.extend_i32_s (global.get $d)))
    (global.set $@stack (i32.sub (global.get $@stack) (i32.const 8)))
    (call $printf (global.get $@stack) (i32.const 2))
    (block $@block_1_break
      (set_local $i (i32.const -7))
      (loop $@block_1_continue
        (br_if $@block_1_break (i32.gt_s (get_local $i) (i32.const 7)))
        (i64.store (global.get $@stack) (i64.const 1069))
        (global.set $@stack (i32.add (global.get $@stack) (i32.const 8)))
        (i64.store (global.get $@stack) (i64.extend_i32_s (get_local $i)))
        (global.set $@stack (i32.add (global.get $@stack) (i32.const 8)))
        (i64.store (global.get $@stack) (i64.extend_i32_s (i32.rem_s (get_local $i) (global.get $d))))
        (global.set $@stack (i32.add (global.get $@stack) (i32.const 8)))
        (i64.store (global.get $@stack) (i64.extend_i32_s (call $mod_ceiling (get_local $i))))
        (global.set $@stack (i32.add (global.get $@stack) (i32.const 8)))
        (i64.store (global.get $@stack) (i64.extend_i32_s (call $mod_floor (get_local $i))))
        (global.set $@stack (i32.sub (global.get $@stack) (i32.const 32)))
        (call $printf (global.get $@stack) (i32.const 5))
        (set_local $i (i32.add (get_local $i) (i32.const 1)))
        (br $@block_1_continue)))
    (global.set $@stack (get_local $@stack_entry))
    (i32.const 0)))
