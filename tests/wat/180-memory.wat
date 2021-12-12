(module
  (import "c4wa" "printf" (func $printf (param i32) (param i32)))
  (global $__mm_capacity (mut i32) (i32.const 0))
  (global $seed (mut i32) (i32.const 57))
  (global $__mm_stat_freed (mut i32) (i32.const 0))
  (global $__mm_inuse (mut i32) (i32.const 0))
  (global $__mm_expand_by (mut i32) (i32.const 10))
  (global $__mm_start (mut i32) (i32.const 0))
  (global $__mm_extra_offset (mut i32) (i32.const -1))
  (global $__mm_stat_allocated (mut i32) (i32.const 0))
  (global $@stack (mut i32) (i32.const 0))
  (global $__mm_first (mut i32) (i32.const -1))
  (global $storage (mut i32) (i32.const 0))
  (global $__mm_size (mut i32) (i32.const -1))
  (memory (export "memory") 1)
  (data (i32.const 1024) "\E2\80\BC\EF\B8\8F ASSERTION: \22unit->integrity == (unit->id ^ 816191)\22 @ line %d\0A\00Starting memory test with %d empty \22unit\22 pointers and %d iterations\0A\00Finished fixed memory test\0A\00A/R/C: %d/%d/%d; CAP: %d/%d\0A\00")
  (func $mulberry32 (result f64)
    (local $t i32)
    (global.set $seed (i32.add (global.get $seed) (i32.const 1831565813)))
    (set_local $t (global.get $seed))
    (set_local $t (i32.mul (i32.xor (get_local $t) (i32.shr_u (get_local $t) (i32.const 15))) (i32.or (get_local $t) (i32.const 1))))
    (set_local $t (i32.xor (get_local $t) (i32.add (get_local $t) (i32.mul (i32.xor (get_local $t) (i32.shr_u (get_local $t) (i32.const 7))) (i32.or (get_local $t) (i32.const 61))))))
    (f64.div (f64.convert_i32_u (i32.xor (get_local $t) (i32.shr_u (get_local $t) (i32.const 14)))) (f64.const 4.294967296E9)))
  (func $allocate (param $id i32) (result i32)
    (local $unit i32)
    (set_local $unit (call $malloc (i32.const 108)))
    (memory.fill (get_local $unit) (i32.const 0) (i32.const 108))
    (i32.store (get_local $unit) (get_local $id))
    (i32.store (i32.add (get_local $unit) (i32.const 4)) (i32.xor (get_local $id) (i32.const 816191)))
    (get_local $unit))
  (func $verify (param $unit i32)
    (local $@stack_entry i32)
    (set_local $@stack_entry (global.get $@stack))
    (if (i32.ne (i32.load (i32.add (get_local $unit) (i32.const 4))) (i32.xor (i32.load (get_local $unit)) (i32.const 816191)))
      (then
        (i64.store (global.get $@stack) (i64.const 1024))
        (global.set $@stack (i32.add (global.get $@stack) (i32.const 8)))
        (i64.store (global.get $@stack) (i64.const 48))
        (global.set $@stack (i32.sub (global.get $@stack) (i32.const 8)))
        (call $printf (global.get $@stack) (i32.const 2))
        (unreachable)))
    (global.set $@stack (get_local $@stack_entry)))
  (func $main (export "main") (result i32)
    (local $@stack_entry i32)
    (local $n_iter i32)
    (local $iter i32)
    (local $idx i32)
    (local $allocated i32)
    (local $freed i32)
    (local $current i32)
    (local $in_use i32)
    (local $capacity i32)
    (set_local $@stack_entry (global.get $@stack))
    (global.set $storage (i32.const 1221))
    (call $mm_init (i32.const 4000) (i32.const 108))
    (memory.fill (global.get $storage) (i32.const 0) (i32.const 4000))
    (set_local $n_iter (i32.const 100000))
    (i64.store (global.get $@stack) (i64.const 1094))
    (global.set $@stack (i32.add (global.get $@stack) (i32.const 8)))
    (i64.store (global.get $@stack) (i64.const 1000))
    (global.set $@stack (i32.add (global.get $@stack) (i32.const 8)))
    (i64.store (global.get $@stack) (i64.extend_i32_s (get_local $n_iter)))
    (global.set $@stack (i32.sub (global.get $@stack) (i32.const 16)))
    (call $printf (global.get $@stack) (i32.const 3))
    (block $@block_1_break
      (loop $@block_1_continue
        (br_if $@block_1_break (i32.ge_s (get_local $iter) (get_local $n_iter)))
        (set_local $idx (i32.trunc_f64_s (f64.mul (call $mulberry32) (f64.const 1000.0))))
        (if (i32.load (i32.add (global.get $storage) (i32.mul (get_local $idx) (i32.const 4))))
          (then
            (call $verify (i32.load (i32.add (global.get $storage) (i32.mul (get_local $idx) (i32.const 4)))))
            (call $free (i32.load (i32.add (global.get $storage) (i32.mul (get_local $idx) (i32.const 4)))))
            (i32.store (i32.add (global.get $storage) (i32.mul (get_local $idx) (i32.const 4))) (i32.const 0)))
          (else
            (i32.store (i32.add (global.get $storage) (i32.mul (get_local $idx) (i32.const 4))) (call $allocate (i32.add (i32.const 1) (get_local $iter))))))
        (set_local $iter (i32.add (get_local $iter) (i32.const 1)))
        (br $@block_1_continue)))
    (i64.store (global.get $@stack) (i64.const 1164))
    (global.set $@stack (i32.sub (global.get $@stack) (i32.const 0)))
    (call $printf (global.get $@stack) (i32.const 1))
    (set_local $allocated (global.get $@stack))
    (global.set $@stack (i32.add (global.get $@stack) (i32.const 4)))
    (set_local $freed (global.get $@stack))
    (global.set $@stack (i32.add (global.get $@stack) (i32.const 4)))
    (set_local $current (global.get $@stack))
    (global.set $@stack (i32.add (global.get $@stack) (i32.const 4)))
    (set_local $in_use (global.get $@stack))
    (global.set $@stack (i32.add (global.get $@stack) (i32.const 4)))
    (set_local $capacity (global.get $@stack))
    (global.set $@stack (i32.add (global.get $@stack) (i32.const 4)))
    (call $mm_stat (get_local $allocated) (get_local $freed) (get_local $current) (get_local $in_use) (get_local $capacity))
    (i64.store (global.get $@stack) (i64.const 1192))
    (global.set $@stack (i32.add (global.get $@stack) (i32.const 8)))
    (i64.store (global.get $@stack) (i64.extend_i32_s (i32.load (get_local $allocated))))
    (global.set $@stack (i32.add (global.get $@stack) (i32.const 8)))
    (i64.store (global.get $@stack) (i64.extend_i32_s (i32.load (get_local $freed))))
    (global.set $@stack (i32.add (global.get $@stack) (i32.const 8)))
    (i64.store (global.get $@stack) (i64.extend_i32_s (i32.load (get_local $current))))
    (global.set $@stack (i32.add (global.get $@stack) (i32.const 8)))
    (i64.store (global.get $@stack) (i64.extend_i32_s (i32.load (get_local $in_use))))
    (global.set $@stack (i32.add (global.get $@stack) (i32.const 8)))
    (i64.store (global.get $@stack) (i64.extend_i32_s (i32.load (get_local $capacity))))
    (global.set $@stack (i32.sub (global.get $@stack) (i32.const 40)))
    (call $printf (global.get $@stack) (i32.const 6))
    (global.set $@stack (get_local $@stack_entry))
    (i32.const 0))
  (func $mm_init (param $extra_offset i32) (param $size i32)
    (if (block $@block_1_break (result i32) (drop (br_if $@block_1_break (i32.const 1) (i32.lt_s (get_local $extra_offset) (i32.const 0)))) (drop (br_if $@block_1_break (i32.const 1) (i32.lt_s (get_local $size) (i32.const 1)))) (drop (br_if $@block_1_break (i32.const 1) (global.get $__mm_start))) (i32.const 0))
      (then
        (unreachable)))
    (global.set $__mm_extra_offset (get_local $extra_offset))
    (global.set $__mm_size (get_local $size))
    (global.set $__mm_start (i32.add (i32.const 1221) (global.get $__mm_extra_offset))))
  (func $malloc (param $size i32) (result i32)
    (local $unit_size i32)
    (local $required i32)
    (local $cur i32)
    (local $j i32)
    (local $result i32)
    (global.set $__mm_stat_allocated (i32.add (global.get $__mm_stat_allocated) (i32.const 1)))
    (if (i32.eqz (global.get $__mm_start))
      (then
        (call $mm_init (i32.const 0) (get_local $size))))
    (if (i32.gt_s (get_local $size) (global.get $__mm_size))
      (then
        (unreachable)))
    (set_local $unit_size (i32.add (i32.const 1) (i32.mul (i32.const 8) (global.get $__mm_size))))
    (if (if (result i32) (i32.ge_s (global.get $__mm_first) (i32.const 0)) (then (i32.const 0)) (else (i32.ne (i32.eq (global.get $__mm_inuse) (global.get $__mm_capacity)) (i32.const 0))))
      (then
        (set_local $required (i32.add (i32.div_s (i32.add (i32.add (i32.const 1221) (global.get $__mm_extra_offset)) (i32.mul (i32.mul (i32.add (global.get $__mm_capacity) (global.get $__mm_expand_by)) (i32.const 8)) (get_local $unit_size))) (i32.const 64000)) (i32.const 1)))
        (if (i32.gt_s (get_local $required) (memory.size))
          (then
            (drop (memory.grow (i32.sub (get_local $required) (memory.size))))))
        (global.set $__mm_capacity (i32.add (global.get $__mm_capacity) (global.get $__mm_expand_by)))))
    (if (i32.lt_s (global.get $__mm_first) (i32.const 0))
      (then
        (if (i32.ge_s (global.get $__mm_inuse) (global.get $__mm_capacity))
          (then
            (unreachable)))
        (i64.store (i32.add (global.get $__mm_start) (i32.mul (i32.mul (global.get $__mm_inuse) (get_local $unit_size)) (i32.const 8))) (i64.const -1))
        (global.set $__mm_first (global.get $__mm_inuse))
        (global.set $__mm_inuse (i32.add (global.get $__mm_inuse) (i32.const 1)))))
    (if (i32.lt_s (global.get $__mm_first) (i32.const 0))
      (then
        (unreachable)))
    (set_local $cur (i32.add (global.get $__mm_start) (i32.mul (i32.mul (global.get $__mm_first) (get_local $unit_size)) (i32.const 8))))
    (if (i64.eq (i64.load (get_local $cur)) (i64.const 0))
      (then
        (unreachable)))
    (set_local $j (i32.wrap_i64 (i64.ctz (i64.load (get_local $cur)))))
    (i64.store (get_local $cur) (i64.xor (i64.load (get_local $cur)) (i64.shl (i64.const 1) (i64.extend_i32_s (get_local $j)))))
    (set_local $result (i32.add (i32.add (get_local $cur) (i32.const 8)) (i32.mul (get_local $j) (global.get $__mm_size))))
    (if (i64.eqz (i64.load (get_local $cur)))
      (then
        (loop $@block_1_continue
          (global.set $__mm_first (i32.add (global.get $__mm_first) (i32.const 1)))
          (br_if $@block_1_continue (if (result i32) (i32.ge_s (global.get $__mm_first) (global.get $__mm_inuse)) (then (i32.const 0)) (else (i32.ne (i64.eqz (i64.load (i32.add (global.get $__mm_start) (i32.mul (i32.mul (global.get $__mm_first) (get_local $unit_size)) (i32.const 8))))) (i32.const 0))))))
        (if (i32.eq (global.get $__mm_first) (global.get $__mm_inuse))
          (then
            (global.set $__mm_first (i32.const -1))))))
    (get_local $result))
  (func $free (param $box i32)
    (local $unit_size i32)
    (local $offset i32)
    (local $idx i32)
    (local $cur i32)
    (local $j i32)
    (global.set $__mm_stat_freed (i32.add (global.get $__mm_stat_freed) (i32.const 1)))
    (set_local $unit_size (i32.add (i32.const 1) (i32.mul (i32.const 8) (global.get $__mm_size))))
    (set_local $offset (i32.sub (get_local $box) (global.get $__mm_start)))
    (set_local $idx (i32.div_s (i32.div_s (get_local $offset) (get_local $unit_size)) (i32.const 8)))
    (set_local $cur (i32.add (global.get $__mm_start) (i32.mul (i32.mul (get_local $idx) (get_local $unit_size)) (i32.const 8))))
    (set_local $j (i32.div_s (i32.sub (i32.sub (get_local $box) (get_local $cur)) (i32.const 8)) (global.get $__mm_size)))
    (if (i32.lt_s (get_local $j) (i32.const 0))
      (then
        (unreachable)))
    (if (i32.ge_s (get_local $j) (i32.const 64))
      (then
        (unreachable)))
    (if (i32.ne (get_local $box) (i32.add (i32.add (get_local $cur) (i32.const 8)) (i32.mul (get_local $j) (global.get $__mm_size))))
      (then
        (unreachable)))
    (if (i32.wrap_i64 (i64.and (i64.load (get_local $cur)) (i64.shl (i64.const 1) (i64.extend_i32_s (get_local $j)))))
      (then
        (unreachable)))
    (i64.store (get_local $cur) (i64.xor (i64.load (get_local $cur)) (i64.shl (i64.const 1) (i64.extend_i32_s (get_local $j)))))
    (if (i32.lt_s (get_local $idx) (global.get $__mm_first))
      (then
        (global.set $__mm_first (get_local $idx)))))
  (func $__mm_count_boxes (result i32)
    (local $unit_size i32)
    (local $res i32)
    (local $i i32)
    (set_local $unit_size (i32.add (i32.const 1) (i32.mul (i32.const 8) (global.get $__mm_size))))
    (block $@block_1_break
      (loop $@block_1_continue
        (br_if $@block_1_break (i32.ge_s (get_local $i) (global.get $__mm_inuse)))
        (set_local $res (i32.add (get_local $res) (i32.sub (i32.const 64) (i32.wrap_i64 (i64.popcnt (i64.load (i32.add (global.get $__mm_start) (i32.mul (i32.mul (get_local $i) (get_local $unit_size)) (i32.const 8)))))))))
        (set_local $i (i32.add (get_local $i) (i32.const 1)))
        (br $@block_1_continue)))
    (get_local $res))
  (func $mm_stat (param $allocated i32) (param $freed i32) (param $current i32) (param $in_use i32) (param $capacity i32)
    (i32.store (get_local $allocated) (global.get $__mm_stat_allocated))
    (i32.store (get_local $freed) (global.get $__mm_stat_freed))
    (i32.store (get_local $current) (call $__mm_count_boxes))
    (i32.store (get_local $in_use) (global.get $__mm_inuse))
    (i32.store (get_local $capacity) (global.get $__mm_capacity))))
