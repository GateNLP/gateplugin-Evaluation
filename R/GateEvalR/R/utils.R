
p_d <- function(x) {
  if(typeof(x) == "double" || typeof(x) == "integer") {
    if(x == 2147483647) {
      return("MAX")
    }
    return(paste(round(x,digits=3)))
  } else {
    return(x)
  }
}

#' Same as [] but if the index is NA, returns NA as the element
#'
getAt <- function(vector,index) {
  if(is.na(index)) return(NA)
  return(vector[index])
}

#' Add the elements of the second list to the first and return the result.
#'
#' Only elements not already there are added, and if additional parameters
#' are given, they are interpreted as element names and only those names
#' are added from the second list.
#'
#' @param l1 the list to which entries should get added
#' @param l2 the list from which entries should get added
#' @param ... names of entries to add from list l2 to l1
#' @return a copy of l1 with the entries from l2, which are not already present, added.
#' (if names were also specified, only entries which correspond to one of those
#' names and are not already present)
add_list_to_list <- function(l1,l2,...) {
  names = list(...)
  if(length(names)==0) {
    names = names(l2)
  }
  for(name in names) {
    if(is.null(l1[[name]])) {
      l1[[name]] = l2[[name]]
    }
  }
  return(l1)
}
