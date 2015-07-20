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
  cat("Fs(99% CI): ",p_d(x$F1StrictCI99l),p_d(x$F1Strict),p_d(x$F1StrictCI99u),"\n")
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
