## IMPORTANT TODOS:
## = the constructor function needs to be able to find out which kind of file
##   is read and use that too to create the appropriate type of object
##   For now, in addition to GateEval, it may return an object of type
##   GateEvalDetail
## = implement print select etc for GateEvalDetail
## = implement the appropriate subclass(es) for the GateEvalDetail we already have
##   (accuracy of list-based tagging for disambiguation)
## = check if GateEvalDetail can inherit from GateEval (so we can use NextMethod for
##   the generic functions?) If that does not work currently we need to restructure
##   the hierarchy so we have GateEval with subclasses GateEvalDetail and
##   GateEvalAccum with the current functionality implemented for Accum.
## = implement tagginglist functions for all list-based classes except the ones
##   which inherit from GateEvalDetail

#' Read a file as created by one of the Evaluation PRs
#'
#' @param filename The name of the file to read.
#' @param evalId The evaluation id to select, if the file contains rows for more than one evaluation id.
#'   If this is not specified, all evaluation ids will be loaded.
#' @return an evaluation container object. The container object allows to access individual evaluation objects
#'   using the \code{get} function.
#' @export
#' @examples
#' \dontrun{
#'   GateEval("thisid.tsv", evalId = "thisid")
#' }
GateEval <- function(filename, evalId=NULL) {
  cat("Loading data from ",filename," ...\n")
  df1 = read.delim(filename,encoding="UTF8", row.names=NULL, as.is=TRUE)
  name = sub("\\.tsv","",basename(filename))
  data=dplyr::as.tbl(df1)
  ids = dplyr::distinct(dplyr::select(data,evaluationId))
  evaltypes = dplyr::distinct(dplyr::select(data,evaluationType))
  ret = list(data=data,filename=filename,ids=ids$evaluationId,
             evaltypes=evaltypes$evaluationType,name=name)
  ## now check if it is a "normal" tsv file or one with Detail information, if
  ## it is the latter, make it a "GateEvalDetail" instance
  ## The simple heuristic we use is by checking if there si a "rankOfStrictMatch" variable
  if("rankOfStrictMatch" %in% colnames(data)) {
    class(ret) <- c("GateEvalDetail","GateEval")
  } else {
    class(ret) <- "GateEval"
  }
  return(ret)
}

#' @export
print.GateEval <- function(x, useS4 = FALSE, ...) {
  cat("EvalTagging from file ",x$filename,"\n")
  cat("Evaluation ids: ",x$ids,"\n")
  cat("Evaluation types: ",x$evaltypes,"\n")
  return(invisible(x))
}

#' @export
print.GateEvalDetail <- function(x, useS4 = FALSE, ...) {
  cat("EvalTaggingDetail from file ",x$filename,"\n")
  cat("Evaluation ids: ",x$ids,"\n")
  cat("Evaluation types: ",x$evaltypes,"\n")
  return(invisible(x))
}


#' Select one specific evaluation.
#'
#' This tries to limit the data for an evaluation to a single evaluation id,
#' evaluation type, and annotation type. This is a necessary step before
#' specific kinds of analyses can be performed. Depending on what is selected,
#' objects of different sub-types of GateEval are returned.
#' It is an error to perform a selection which leaves nothing or more than
#' one specific evalaution.
#'
#' @export
#' @param x an existing evaluation object containing one or more evaluations.
#' @param ... any additional parameters
#' @return an object representing the selected evaluation instance
select <- function(x, ...) {
  if(is.null(attr(x,"class"))) {
    cat("select not usable\n")
  }
  UseMethod("select",x)
}

## TODO not sure yet how to handle this best ...
## select.default <- get("select", mode="function")

#' Select a specific evaluation id and type from the initial evaluation object.
#'
#' Either evaluationId or evaluationType or both must be specified. If there
#' is no data for the given id and/or type, the method shows a warning and
#' returns NULL. Otherwise the method returns an object of a class specific
#' to the evaluation type.
#'
#' NOTE: at the moment the data after performing the select must be restricted
#' to a single evaluationId, a single evaluationType and a single
#' annotationType (the special symbold "*" can be used to restrict the annotation
#' type to "all")
#'
#' TODO: restricting to annotation type not yet implemented!!
#'
#' @param x The object created by GateEval
#' @param evaluationId A string that identifies the desired evaluatiob Id
#' @param evaluationType A string that identifies the desired evaluation type
#' @param annotationType A string the identifies the annotation type
#' @return An object of some subclass of GateEval, depending on the kind of
#' selected evaluation instance.
select.GateEval <- function(x, evaluationType = NULL, evaluationId=NULL, annotationType = NULL) {
  obj <- x$data
  ret <- x

  ## we limit separately by id and type so we can identify if the id or type
  ## refer to something that is not there ...
  if(!is.null(evaluationId)) {
    tmp <- evaluationId
    obj <- dplyr::filter(obj, evaluationId == tmp)
    if(dim(obj)[1] == 0) {
      stop("No data found for evaluationId ",evaluationId)
    }
  }
  ## check if we already have exactly one id, if not, we have an error
  ids = dplyr::distinct(dplyr::select(obj,evaluationId))
  if(dim(ids)[1] != 1) {
    stop("Not exactly one evaluationId left but "+dim(ids)[1],": ",ids$evaluationId)
  } else {
    ret$ids=ids$evaluationId
  }

  if(!is.null(evaluationType)) {
    ## TODO: check if it is a known evaluation type ... we do not allow
    ## any others!!
    tmp <- evaluationType
    obj <- dplyr::filter(obj, evaluationType == tmp)
    if(dim(obj)[1] == 0) {
      stop("No data found for evaluationType ",evaluationType)
    }
  }
  ## check if we already have exactly one id, if not, we have an error
  ids = dplyr::distinct(dplyr::select(obj,evaluationType))
  if(dim(ids)[1] != 1) {
    stop("Not exactly one evaluationType left but ",dim(ids)[1],": ",ids$evaluationType)
  }
  ret$evaltypes=ids$evaluationType

  if(!is.null(annotationType)) {
    tmp <- annotationType
    obj <- dplyr::filter(obj, annotationType == tmp)
    if(dim(obj)[1] == 0) {
      stop("No data found for annotationType ",annotationType)
    }
  }
  ## check if we already have exactly one id, if not, we have an error
  ids = dplyr::distinct(dplyr::select(obj,annotationType))
  if(dim(ids)[1] != 1) {
    ## TODO: for now we ignore this if the evaluation type starts with list-
    ## It seems that we sometimes get things like Mention=Lookup and Mention=LookupList
    ## This could be a big in the PR!
    if(!gdata::startsWith(ret$evaltypes[1],"list-")) {
      stop("Not exactly one annotationType left but ",dim(ids)[1],": ",ids$annotationType)
    }
  }
  ret$annotationType=ids$annotationType


  ## The class of the object will be assigned based on the evaluation type
  ## This is done using a helper function, class_for_type(type)
  ret$data <- obj
  class(ret) <- class_for_type(evaluationType)
  debug_preInit<<-ret
  ret = initializeObject(ret)
  return(ret)
}

#' Internal generic function for initializing an evaluation instance.
#'
#' @param x the object to initialize
#' @param ... additional parameters
#' @return the initialized object
initializeObject <- function(x, ...) {
  if(is.null(attr(x,"class"))) {
    cat("initializeObject not usable\n")
  }
  UseMethod("initializeObject",x)
}


summary.GateEval <- function(x,...) {
  ## TODO: this should return an object that provides a useful summary for the
  ## the evaluation. The object could in turn have a specialized print method
  ## for showing that summary to the screen, but everything should be accesible
  ## through named fields.
  ret = x
  class(ret) = "summary.GateEval"
  ret
}


plot.GateEval <- function(x, ...) {
  ##
  invisible()
}
