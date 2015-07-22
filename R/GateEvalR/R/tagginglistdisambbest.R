## Functions for the TaggingListDisambBest class.
## We only use the F columns (P/R/F are always identical because they all measure
## disambiguation accuracy really)

## NOTE: "best" means the best guess i.e. the guess at rank 0

#' Print a TaggingListDisambBest object.
#'
#' @param x the object to print
#' @param ...  additional parameters.
#' @export
print.TaggingListDisambBest <- function(x,...) {
  NextMethod("print")
  cat("TaggingListDisambBest [micro averages not implemented yet]",
      "\n")
  cat("As(99% CI): ",p_d(x$accuracyStrictAt0CI99l),p_d(x$accuracyStrictAt0CI99u),"\n")
  cat("Al(99% CI): ",p_d(x$accuracyLenientAt0CI99l),p_d(x$accuracyLenientAt0CI99u),"\n")
  return(invisible(x))
}

#' Initialize an object of type TaggingListBest
#'
#' @param x the object to initialize
#' @return the initialized object
initializeObject.TaggingListDisambBest <- function(x) {
  ## NOTE: at the moment we do not have a micro average line for this!!

  ## calculate bootstrapping based CIs and add to the object.
  ## We always calculate CIs for 0.90, 0.95 and 0.99
  ## The number of samples is 10000
  ## TODO: make the number of samples configurable by some global options??
  ## the field names for the CIs are <originalName>CI90l, CI90u, CI95l etc
  ## The field names for the original bootstrapping result objects are <originalName>Boot
  ## debug_beforeboot<<-x$data
  bootobjects=bootPRF(x$data)
  x = add_list_to_list(x,bootobjects)
  x$accuracyStrictAt0CI99l = bootobjects$F1StrictCI99l
  x$accuracyStrictAt0CI99u = bootobjects$F1StrictCI99u
  x$accuracyLenientAt0CI99l = bootobjects$F1LenientCI99l
  x$accuracyLenientAt0CI99u = bootobjects$F1LenientCI99u
  return(x)
}

#' Plot an object of type TaggingListDisambBest
#'
#' This will plot a box plot of all the per-document F1 measures.
#'
#' @param x the object to plot
#' @param strict if TRUE plot the strict measures, otherwise the lenient measures
#' @param addstripchart if TRUE overlay the box plot with a strip chart
plot.TaggingListDisambBest <- function(x, strict=TRUE, addstripchart=FALSE, ...) {
  obj = dplyr::filter(x$data,!grepl("^\\[.+\\]$",docName,perl=TRUE))
  if(strict) {
    f = obj$F1Strict
    l = "Accuracy Strict"
  } else {
    f = obj$F1Lenient
    l = "Accuracy Lenient"
  }
  boxplot(f,ylab=l,...)
  if(addstripchart) {
    stripchart(f,vertical=TRUE,method="jitter",add=TRUE)
  }
  ## TODO: maybe useful, from library psych, to show the bootstrapping CIs
  ## error.bars(add=TRUE)
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
