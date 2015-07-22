## Functions for the TaggingListRank class.

#' Print a TaggingListRank object.
#'
#' @param x the object to print
#' @param ...  additional parameters.
#' @export
print.TaggingListRank <- function(x,...) {
  NextMethod("print")
  cat("TaggingListRank, @maxFs: Ps/Rs/Fs Pl/Rl/Fl",
      paste(sep="/",p_d(x$precisionStrict),p_d(x$recallStrict),p_d(x$F1Strict)),
      paste(sep="/",p_d(x$precisionLenient),p_d(x$recallLenient),p_d(x$F1Lenient)),
      " at rank th ",p_d(x$maxF1StrictRank),
      "\n")
  return(invisible(x))
}

#' Initialize an object of type Tagging Score
#'
#' @param x the object to initialize
#' @return the initialized object
initializeObject.TaggingListRank <- function(x) {
  obj <- x$data
  obj <- dplyr::filter(obj, docName == "[doc:all:micro]")
  obj <- dplyr::filter(obj, !is.infinite(threshold))
  x$data = obj
  ## find the row with the maximum strict f value
  i=which.max(obj$F1Strict)
  x$maxF1StrictRank = obj$threshold[i]
  row = obj[i,]
  x = add_list_to_list(x,row)
  return(x)
}

#' Plot an object of type TaggingListRank
#'
#' @param show either "t" to show the threshold or "f" to show the f measure
plot.TaggingListRank <- function(x, strict=TRUE, show="t", ...) {
  if(strict) {
    r = x$data$recallStrict
    p = x$data$precisionStrict
    f = signif(x$data$F1Strict,digits=2)
    lx = "Recall Strict"
    ly = "Precision Strict"
  } else {
    r = x$data$recallLenient
    p = x$data$precisionLenient
    f = signif(x$data$F1Lenient,digits=2)
    lx = "Recall Lenient"
    ly = "Precision Lenient"
  }
  th=x$data$threshold
  plot(r,p,xlab=lx,ylab=ly)
  lines(r,p,type="l")
  if(show=="t") {
    text(r,p,labels=th,cex=0.7,pos=3,...)
  } else if(show=="f") {
    text(r,p,labels=f,cex=0.7,pos=3,...)
  } else {
    stop("Parameter show must be either 't' or 'f'")
  }
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
