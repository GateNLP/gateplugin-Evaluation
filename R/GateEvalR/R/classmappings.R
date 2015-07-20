class_for_type <- function(type) {
  if(type == "normal") {
    return(c("TaggingBasic","GateEval"))
  } else if(type == "xxx") {
    return("XXX")
  } else {
    stop("No class known for evaluation type "+type)
  }
}
