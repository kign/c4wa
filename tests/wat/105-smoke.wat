(module
  (import "c4wa" "printf" (func $printf (param i32) (param i32)))
  (global $@stack (mut i32) (i32.const 8))
  (global $N i32 (i32.const 100))
  (memory (export "memory") 1)
  (data (i32.const 1024) "2\00 %d\00\0A\00")
  (func $v1
    (local $@stack_entry i32)
    (local $p i32)
    (local $found i32)
    (local $d i32)
    (set_local $@stack_entry (global.get $@stack))
    (call $printf (i32.const 1024) (global.get $@stack))
    (block $@block_1_break
      (set_local $p (i32.const 3))
      (loop $@block_1_continue
        (br_if $@block_1_break (i32.ge_s (get_local $p) (global.get $N)))
        (set_local $found (i32.const 0))
        (block $@block_1_1_break
          (set_local $d (i32.const 3))
          (loop $@block_1_1_continue
            (br_if $@block_1_1_break (i32.gt_s (i32.mul (get_local $d) (get_local $d)) (get_local $p)))
            (if (i32.eqz (i32.rem_s (get_local $p) (get_local $d)))
              (then
                (set_local $found (i32.const 1))
                (br $@block_1_1_break)))
            (set_local $d (i32.add (get_local $d) (i32.const 2)))
            (br $@block_1_1_continue)))
        (if (i32.eqz (get_local $found))
          (then
            (i64.store align=8 (global.get $@stack) (i64.extend_i32_s (get_local $p)))
            (call $printf (i32.const 1026) (global.get $@stack))))
        (set_local $p (i32.add (get_local $p) (i32.const 2)))
        (br $@block_1_continue)))
    (call $printf (i32.const 1030) (global.get $@stack))
    (global.set $@stack (get_local $@stack_entry)))
  (func $v2
    (local $@stack_entry i32)
    (local $p i32)
    (local $d i32)
    (set_local $@stack_entry (global.get $@stack))
    (call $printf (i32.const 1024) (global.get $@stack))
    (block $@block_1_break
      (set_local $p (i32.const 3))
      (loop $@block_1_continue
        (br_if $@block_1_break (i32.ge_s (get_local $p) (global.get $N)))
        (block $@block_1_1_break
          (set_local $d (i32.const 3))
          (loop $@block_1_1_continue
            (br_if $@block_1_1_break (i32.or (i32.gt_s (i32.mul (get_local $d) (get_local $d)) (get_local $p)) (i32.eq (i32.rem_s (get_local $p) (get_local $d)) (i32.const 0))))
            (set_local $d (i32.add (get_local $d) (i32.const 2)))
            (br $@block_1_1_continue)))
        (if (i32.gt_s (i32.mul (get_local $d) (get_local $d)) (get_local $p))
          (then
            (i64.store align=8 (global.get $@stack) (i64.extend_i32_s (get_local $p)))
            (call $printf (i32.const 1026) (global.get $@stack))))
        (set_local $p (i32.add (get_local $p) (i32.const 2)))
        (br $@block_1_continue)))
    (call $printf (i32.const 1030) (global.get $@stack))
    (global.set $@stack (get_local $@stack_entry)))
  (func $v3
    (local $@stack_entry i32)
    (local $p i32)
    (set_local $@stack_entry (global.get $@stack))
    (call $printf (i32.const 1024) (global.get $@stack))
    (block $@block_1_break
      (set_local $p (i32.const 3))
      (loop $@block_1_continue
        (br_if $@block_1_break (i32.ge_s (get_local $p) (global.get $N)))
        (if (call $test (get_local $p))
          (then
            (i64.store align=8 (global.get $@stack) (i64.extend_i32_s (get_local $p)))
            (call $printf (i32.const 1026) (global.get $@stack))))
        (set_local $p (i32.add (get_local $p) (i32.const 2)))
        (br $@block_1_continue)))
    (call $printf (i32.const 1030) (global.get $@stack))
    (global.set $@stack (get_local $@stack_entry)))
  (func $v4
    (local $@stack_entry i32)
    (local $p i32)
    (local $found i32)
    (local $d i32)
    (set_local $@stack_entry (global.get $@stack))
    (call $printf (i32.const 1024) (global.get $@stack))
    (block $@block_1_break
      (set_local $p (i32.const 3))
      (loop $@block_1_continue
        (br_if $@block_1_break (i32.ge_s (get_local $p) (global.get $N)))
        (set_local $found (i32.const 0))
        (block $@block_1_1_break
          (set_local $d (i32.const 3))
          (loop $@block_1_1_continue
            (br_if $@block_1_1_break (i32.gt_s (i32.mul (get_local $d) (get_local $d)) (get_local $p)))
            (if (i32.gt_s (i32.rem_s (get_local $p) (get_local $d)) (i32.const 0))
              (then
                (set_local $d (i32.add (get_local $d) (i32.const 2)))
                (br $@block_1_1_continue)))
            (set_local $found (i32.const 1))
            (br $@block_1_1_break)
            (set_local $d (i32.add (get_local $d) (i32.const 2)))
            (br $@block_1_1_continue)))
        (if (i32.eqz (get_local $found))
          (then
            (i64.store align=8 (global.get $@stack) (i64.extend_i32_s (get_local $p)))
            (call $printf (i32.const 1026) (global.get $@stack))))
        (set_local $p (i32.add (get_local $p) (i32.const 2)))
        (br $@block_1_continue)))
    (call $printf (i32.const 1030) (global.get $@stack))
    (global.set $@stack (get_local $@stack_entry)))
  (func $v5
    (local $@stack_entry i32)
    (local $p i32)
    (local $found i32)
    (local $d i32)
    (set_local $@stack_entry (global.get $@stack))
    (call $printf (i32.const 1024) (global.get $@stack))
    (block $@block_1_break
      (set_local $p (i32.const 3))
      (loop $@block_1_continue
        (br_if $@block_1_break (i32.ge_s (get_local $p) (global.get $N)))
        (set_local $found (i32.const 0))
        (block $@block_1_1_break
          (set_local $d (i32.const 3))
          (loop $@block_1_1_continue
            (br_if $@block_1_1_break (i32.gt_s (i32.mul (get_local $d) (get_local $d)) (get_local $p)))
            (set_local $found (i32.const 1))
            (if (i32.gt_s (i32.rem_s (get_local $p) (get_local $d)) (i32.const 0))
              (then
                (set_local $found (i32.const 0))
                (set_local $d (i32.add (get_local $d) (i32.const 2)))
                (br $@block_1_1_continue)))
            (br $@block_1_1_break)
            (set_local $d (i32.add (get_local $d) (i32.const 2)))
            (br $@block_1_1_continue)))
        (if (i32.eqz (get_local $found))
          (then
            (i64.store align=8 (global.get $@stack) (i64.extend_i32_s (get_local $p)))
            (call $printf (i32.const 1026) (global.get $@stack))))
        (set_local $p (i32.add (get_local $p) (i32.const 2)))
        (br $@block_1_continue)))
    (call $printf (i32.const 1030) (global.get $@stack))
    (global.set $@stack (get_local $@stack_entry)))
  (func $test (param $p i32) (result i32)
    (local $d i32)
    (if (i32.eq (get_local $p) (i32.const 3))
      (then
        (return (i32.const 1))))
    (set_local $d (i32.const 3))
    (block $@block_1_break
      (loop $@block_1_continue
        (br_if $@block_1_break (i32.or (i32.gt_s (i32.mul (get_local $d) (get_local $d)) (get_local $p)) (i32.eq (i32.rem_s (get_local $p) (get_local $d)) (i32.const 0))))
        (set_local $d (i32.add (get_local $d) (i32.const 2)))
        (br $@block_1_continue)))
    (i32.ne (i32.rem_s (get_local $p) (get_local $d)) (i32.const 0)))
  (func $main (export "main") (result i32)
    (call $v1)
    (call $v2)
    (call $v3)
    (call $v4)
    (call $v5)
    (i32.const 0)))
