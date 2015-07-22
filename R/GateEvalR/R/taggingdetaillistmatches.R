## Functions for the TaggingDetailListMatches class.

#' Print a TaggingDetailListMatches object.
#'
#' @param x the object to print
#' @param ...  additional parameters.
#' @export
print.TaggingDetailListMatches <- function(x,...) {
  NextMethod("print")
  cat("TaggingDetailListMatches, [NOT YET IMPLEMENTED]",
      "\n")
  return(invisible(x))
}

#' Initialize an object of type TaggingDetailListMatches
#'
#' @param x the object to initialize
#' @return the initialized object
initializeObject.TaggingDetailListMatches <- function(x) {
  ## TODO: calculate micro summaries?
  x
}

#' Plot an object of type TaggingListBest
#'
#' By default this shows the distribution of the rank of the first strict
#' match, for all rows which do have a strict match
plot.TaggingDetailListMatches <- function(x, strict=TRUE, maxRank = NA, minRank = NA,   ...) {
  maxr = 99999999
  minr = 0
  if(!is.na(maxRank)) {
    maxr = maxRank
  }
  if(!is.na(minRank)) {
    minr = minRank
  }
  cat("Number of samples initially: ",length(x$data$rankOfStrictMatch),"\n")
  obj = x$data
  if(strict) {
    obj = dplyr::filter(obj,rankOfStrictMatch >= 0) ## get rid of all rows which do not have a strict match at all
    ## limit to those rows where the strict match occurs at a rank >= minRank
    obj = dplyr::filter(obj,rankOfStrictMatch >= minr)
    ## limit to those rows where the strict match occurs at a rank <= maxRank
    obj = dplyr::filter(obj,rankOfStrictMatch <=maxr)
    ranks = obj$rankOfStrictMatch
    l = "Frequency of rank of first strict match"
  } else {
    obj = dplyr::filter(obj,rankOfStrictMatch >= 0 | rankOfPartialMatch >= 0)
    ## insert a new column with the rank of lenient match: the smaller of the strict and partial
    ## which is not -1
    obj$rankOfLenientMatch = mapply(
      function(s,p) {
        l = s;
        if(l==-1) { l = p}
        if(p>-1 && p<l) { l = p}
        return(l)
        }, obj$rankOfStrictMatch, obj$rankOfPartialMatch)
    obj = dplyr::filter(obj,rankOfLenientMatch >= minr)
    obj = dplyr::filter(obj,rankOfLenientMatch <= maxr)
    ranks = obj$rankOfLenientMatch
    l = "Frequency of rank of first lenient match"
  }
  cat("Number of samples remaining: ",length(ranks),"\n")
  hist(ranks,breaks=min(100,length(ranks)),ylab=l,xlab="Rank",...)
  invisible()
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
