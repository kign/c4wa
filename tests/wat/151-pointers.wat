(module
  (import "c4wa" "printf" (func $printf (param i32) (param i32)))
  (global $N i32 (i32.const 100))
  (memory (export "memory") 1)
  (data (i32.const 1024) "%d \00%d\5Cn\00")
  (func $main (export "main") (result i32)
    (local $i i32)
    (local $primes i32)
    (local $n i32)
    (local $p i32)
    (set_local $primes (i32.const 2048))
    (i32.store (i32.add (get_local $primes) (i32.mul (i32.const 0) (i32.const 4))) (i32.const 2))
    (i32.store (i32.add (get_local $primes) (i32.mul (i32.const 1) (i32.const 4))) (i32.const 1))
    (set_local $n (i32.const 1))
    (block $@block_1_break
      (set_local $p (i32.const 3))
      (loop $@block_1_continue
        (br_if $@block_1_break (i32.ge_s (get_local $p) (global.get $N)))
        (block $@block_1_1_break
          (set_local $i (i32.const 1))
          (loop $@block_1_1_continue
            (br_if $@block_1_1_break (i32.or (i32.or (i32.ge_s (get_local $i) (get_local $n)) (i32.ge_s (i32.mul (i32.load (i32.add (get_local $primes) (i32.mul (get_local $i) (i32.const 4)))) (i32.load (i32.add (get_local $primes) (i32.mul (get_local $i) (i32.const 4))))) (get_local $p))) (i32.eq (i32.rem_s (get_local $p) (i32.load (i32.add (get_local $primes) (i32.mul (get_local $i) (i32.const 4))))) (i32.const 0))))
            (set_local $i (i32.add (get_local $i) (i32.const 1)))
            (br $@block_1_1_continue)))
        (if (i32.or (i32.ge_s (get_local $i) (get_local $n)) (i32.ne (i32.rem_s (get_local $p) (i32.load (i32.add (get_local $primes) (i32.mul (get_local $i) (i32.const 4))))) (i32.const 0)))
          (then
            (i32.store (i32.add (get_local $primes) (i32.mul (get_local $n) (i32.const 4))) (get_local $p))
            (set_local $n (i32.add (get_local $n) (i32.const 1)))))
        (set_local $p (i32.add (get_local $p) (i32.const 2)))
        (br $@block_1_continue)))
    (block $@block_2_break
      (set_local $i (i32.const 0))
      (loop $@block_2_continue
        (br_if $@block_2_break (i32.ge_s (get_local $i) (i32.sub (get_local $n) (i32.const 1))))
        (i64.store (i32.const 0) (i64.extend_i32_s (i32.const 1024)))
        (i64.store (i32.const 8) (i64.extend_i32_s (i32.load (i32.add (get_local $primes) (i32.mul (get_local $i) (i32.const 4))))))
        (call $printf (i32.const 0) (i32.const 2))
        (set_local $i (i32.add (get_local $i) (i32.const 1)))
        (br $@block_2_continue)))
    (i64.store (i32.const 0) (i64.extend_i32_s (i32.const 1028)))
    (i64.store (i32.const 8) (i64.extend_i32_s (i32.load (i32.add (get_local $primes) (i32.mul (i32.sub (get_local $n) (i32.const 1)) (i32.const 4))))))
    (call $printf (i32.const 0) (i32.const 2))
    (return (i32.const 0))))
