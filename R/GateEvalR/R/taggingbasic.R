## Functions for the TaggingBasic class.

#' Print a TaggingBasic object.
#'
#' @param x the object to print
#' @param ...  additional parameters.
#' @export
print.TaggingBasic <- function(x,...) {
  NextMethod("print")
  cat("TaggingBasic, Ps/Rs/Fs Pl/Rl/Fl",
      paste(sep="/",p_d(x$precisionStrict),p_d(x$recallStrict),p_d(x$F1Strict)),
      paste(sep="/",p_d(x$precisionLenient),p_d(x$recallLenient),p_d(x$F1Lenient)),
      "\n")
  cat("Ps(99% CI): ",p_d(x$PrecisionStrictCI99l),p_d(x$PrecisionStrict),p_d(x$PrecisionStrictCI99u),"\n")
  cat("Rs(99% CI): ",p_d(x$RecallStrictCI99l),p_d(x$RecallStrict),p_d(x$RecallStrictCI99u),"\n")
  cat("Fs(99% CI): ",p_d(x$F1StrictCI99l),p_d(x$F1Strict),p_d(x$F1StrictCI99u),"\n")
  cat("Pl(99% CI): ",p_d(x$PrecisionLenientCI99l),p_d(x$PrecisionLenient),p_d(x$PrecisionLenientCI99u),"\n")
  cat("Rl(99% CI): ",p_d(x$RecallLenientCI99l),p_d(x$RecallLenient),p_d(x$RecallLenientCI99u),"\n")
  cat("Fl(99% CI): ",p_d(x$F1LenientCI99l),p_d(x$F1Lenient),p_d(x$F1LenientCI99u),"\n")
  return(invisible(x))
}

#' Initialize an object of type Tagging Basic
#'
#' @param x the object to initialize
#' @return the initialized object
initializeObject.TaggingBasic <- function(x) {
  obj <- dplyr::filter(x$data, docName == "[doc:all:micro]")
  if(dim(obj)[1]!=1) {
    stop("Odd TaggingBasic object: number of rows for doc:all:micro not 1 but ",dim(obj)[1],": \n",obj)
  }
  row=obj[1,]
  x = add_list_to_list(x,row)

  ## calculate bootstrapping based CIs and add to the object.
  ## We always calculate CIs for 0.90, 0.95 and 0.99
  ## The number of samples is 10000
  ## TODO: make the number of samples configurable by some global options??
  ## the field names for the CIs are <originalName>CI90l, CI90u, CI95l etc
  ## The field names for the original bootstrapping result objects are <originalName>Boot
  bootobjects=bootPRF(x$data)
  x = add_list_to_list(x,bootobjects)
  return(x)
}

#' Plot an object of type TaggingBasic
#'
plot.TaggingBasic <- function(x, strict=TRUE, addstripchart=FALSE, ...) {
  obj = dplyr::filter(x$data,!grepl("^\\[.+\\]$",docName,perl=TRUE))
  if(strict) {
    f = obj$F1Strict
    l = "F1.0 Strict"
  } else {
    f = obj$F1Lenient
    l = "F1.0 Lenient"
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
