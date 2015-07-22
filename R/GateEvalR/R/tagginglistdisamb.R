## Functions for the TaggingListDisamb class.
##
## For this class, we are only interested in one of P/R/F because all three
## are always identical and really represent maximum accuracy up to the given
## rank threshold.


#' Print a TaggingListDisamb object.
#'
#' @param x the object to print
#' @param ...  additional parameters.
#' @export
print.TaggingListDisamb <- function(x,...) {
  NextMethod("print")
  cat("TaggingListDisamb, As and Al @ 0/10/100",
      paste(sep="/",p_d(x$accuracyStrictAt0),p_d(x$accuracyStrictAt10),p_d(x$accuracyStrictAt100)),
      paste(sep="/",p_d(x$accuracyLenientAt0),p_d(x$accuracyLenientAt10),p_d(x$accuracyLenientAt100)),
      "\n")
  return(invisible(x))
}

#' Initialize an object of type Tagging List Disamb
#'
#' @param x the object to initialize
#' @return the initialized object
initializeObject.TaggingListDisamb <- function(x) {
  obj <- x$data
  obj <- dplyr::filter(obj, docName == "[doc:all:micro]")
  obj <- dplyr::filter(obj, !is.infinite(threshold))
  x$data = obj
  ## find the specific accuracies at 0, 10, 100 and MAX, if not there, set to NA
  asAt0 = getAt(obj$F1Strict,match(0,obj$threshold))
  asAt10 = getAt(obj$F1Strict,match(10,obj$threshold))
  asAt100 = getAt(obj$F1Strict,match(100,obj$threshold))
  alAt0 = getAt(obj$F1Lenient,match(0,obj$threshold))
  alAt10 = getAt(obj$F1Lenient,match(10,obj$threshold))
  alAt100 = getAt(obj$F1Lenient,match(100,obj$threshold))
  tmpl = list(
    accuracyStrictAt0=asAt0,accuracyStrictAt10=asAt10,accuracyStrictAt100=asAt100,
    accuracyLenientAt0=alAt0,accuracyLenientAt10=alAt10,accuracyLenientAt100=alAt100
  )
  x = add_list_to_list(x,tmpl)
  return(x)
}

#' Plot an object of type TaggingListDisamb
#'
#' @param show either "t" to show the threshold or "f" to show the f measure
plot.TaggingListDisamb <- function(x, strict=TRUE, maxRank = NA, ...) {
  if(strict) {
    a = x$data$F1Strict
  } else {
    a = x$data$F1Lenient
  }
  if(!is.na(maxRank)) {
    if(maxRank < length(a)) {
      a = a[1:maxRank]
    }
  }
  th=x$data$threshold
  if(!is.na(maxRank)) {
    if(maxRank < length(th)) {
      th = th[1:maxRank]
    }
  }
  plot(th,a,xlab="Rank",ylab="Maximum Accuracy",...)
  lines(th,a,type="l")
}

## TODO: implement summary.TYPE and plot.TYPE
## Also, decide on a number of methods that make it easier to access important
## measures, e.g. m_fl(obj) or m_fs(beta=0.5) or m_cifsl(conf=0.90) or m_ciplu
## of m_cifsr() (for range) etc.?
## All of these could be vectorized, so if they get a vector of evaluation
## objects, the return a vector of measures?
## Or, a vectorized function measures(objs,measurenames,...) which could return
## a matrix or data.frame such that the rows correspond to the objects and
## the columns correspond to the measures. The clou would be that the measures
## vector could allow for the actual names but also for all kinds of short forms.
## If objs is a single object, it would return a list instead of a data frame (?)
## if objs is a single object and there is only one measure, it would only
## return a single value (a vector, but we could still have a name for the value)
