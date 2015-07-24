class_for_type <- function(type) {
  if(type == "normal") {
    c("TaggingBasic","GateEval")
  } else if(type == "score") {
    c("TaggingScore","GateEval")
  } else if(type == "list-best") {
    c("TaggingListBest","GateEval")
  } else if(type == "list-disamb-best") {
    c("TaggingListDisambBest","GateEval")
  } else if(type == "list-rank") {
    c("TaggingListRank","GateEval")
  } else if(type == "list-score") {
    c("TaggingListScore","GateEval")
  } else if(type == "list-disamb") {
    c("TaggingListDisamb","GateEval")
  } else if(type == "list-matches") {
    c("TaggingDetailListMatches", "GateEvalDetail", "GateEval")
  } else {
    stop("No class known for evaluation type "+type)
  }
}
