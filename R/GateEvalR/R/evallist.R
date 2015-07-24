## Functions that simplify loading and comparing a number of evaluations of
## the same type

## TODO: eventually, each individual evaluation type will have two or three
## lists of values: for printing, for summarization and for details
## (with the generic details() function)
## Then, a list of evaluation instances will produce a list of output
## for print, summary and details, but we also have additional methods
## to e.g. create data frames which can then in turn get converted to
## latex tables or plotted

#' Read in a list of evaluation files and select a specific evaluation type, id or annotation type.
#'
#' NOTE: the evaluation instances are stored by a name "eval<N>" where "N" is the
#' index of the file.
#'
#' @param fileNames a vector of file names to read in
#' @return an object that contains the list of evaluations
GateEvalList <- function(fileNames,evaluationType=NULL,evaluationId=NULL,annotationType=NULL) {
  instances = list()
  i = 1
  for(fileName in fileNames) {
    tryCatch(
      { tmp = select(GateEval(fileName),evaluationType,evaluationId,annotationType)
        instances[[paste("eval",i,sep="")]] = tmp
        i = i + 1
      },
      error = function(e) { 
        print(paste("Error loading ",fileName,", ignored! ",e$message)) 
      }
    )
  }
  ret = list(datas=instances,fileNames=fileNames)
  class(ret) = "GateEvalList"
  ret
}


## TODO
print.GateEvalList = function(x,...) {
  for(el in x$datas) {
    print(el)
  }
}
