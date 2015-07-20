library("GateEvalR")

# the first argument to expect_equal should be a function call
test_that("description of test", { 
  expect_equal("myfunction","myfunction") 
})
