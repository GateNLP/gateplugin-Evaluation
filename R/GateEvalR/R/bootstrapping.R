
#' Return a list of bootstrap objects for the precision and recall values
#'
#'
#' If the data dataframe does not contain the necessary rows for individual
#' documents, an error is raised.
#'
#' @param data expected to be a data frame as stored within an evaluation object.
bootPRF <- function(data) {
  cat("Calculating bootstrapping estimates: ")
  N = 10000
  bydocs = dplyr::filter(data,is.nan(threshold))
  bydocs = dplyr::filter(data,!grepl("^\\[.+\\]$",docName,perl=TRUE))
  ## check if we have more than one row left
  if(dim(bydocs)[1] < 1) {
    stop("Cannot perform bootstrapping, no rows left for ",data)
  }
  ret = list()
  ## for each parameter, we need to create a temporary data frame df which
  ## containes the correct columns

  ## TODO: find out which kind of interval conforms to the CIs we get from
  ## the TAC evaluation tool and use that!
  ## For now we use

  oldopts=options(warn=-1)
  ## recall strict
  cat(" Rs")
  df_tmp = data.frame(n=bydocs$targets,ncs=bydocs$correctStrict)
  boot_tmp = boot::boot(df_tmp,boot_tmp_f1,N)
  ret$recallStrictBoot = boot_tmp
  cis_tmp = boot::boot.ci(boot_tmp,conf=c(0.90,0.95,0.99),,type="all")
  ret$recallStrictCI90l = cis_tmp$basic[1,4]
  ret$recallStrictCI90u = cis_tmp$basic[1,5]
  ret$recallStrictCI95l = cis_tmp$basic[2,4]
  ret$recallStrictCI95u = cis_tmp$basic[2,5]
  ret$recallStrictCI99l = cis_tmp$basic[3,4]
  ret$recallStrictCI99u = cis_tmp$basic[3,5]

  ## recall lenient
  cat(" Rl")
  df_tmp = data.frame(n=bydocs$targets,ncs=(bydocs$correctPartial+bydocs$correctStrict))
  boot_tmp = boot::boot(df_tmp,boot_tmp_f1,N)
  ret$recallLenientBoot = boot_tmp
  cis_tmp = boot::boot.ci(boot_tmp,conf=c(0.90,0.95,0.99),,type="all")
  ret$recallLenientCI90l = cis_tmp$basic[1,4]
  ret$recallLenientCI90u = cis_tmp$basic[1,5]
  ret$recallLenientCI95l = cis_tmp$basic[2,4]
  ret$recallLenientCI95u = cis_tmp$basic[2,5]
  ret$recallLenientCI99l = cis_tmp$basic[3,4]
  ret$recallLenientCI99u = cis_tmp$basic[3,5]

  ## precision strict
  cat(" Ps")
  df_tmp = data.frame(n=bydocs$responses,ncs=bydocs$correctStrict)
  boot_tmp = boot::boot(df_tmp,boot_tmp_f1,N)
  ret$precisionStrictBoot = boot_tmp
  cis_tmp = boot::boot.ci(boot_tmp,conf=c(0.90,0.95,0.99),,type="all")
  ret$precisionStrictCI90l = cis_tmp$basic[1,4]
  ret$precisionStrictCI90u = cis_tmp$basic[1,5]
  ret$precisionStrictCI95l = cis_tmp$basic[2,4]
  ret$precisionStrictCI95u = cis_tmp$basic[2,5]
  ret$precisionStrictCI99l = cis_tmp$basic[3,4]
  ret$precisionStrictCI99u = cis_tmp$basic[3,5]

  ## precision lenient
  cat(" Pl")
  df_tmp = data.frame(n=bydocs$responses,ncs=(bydocs$correctPartial+bydocs$correctStrict))
  boot_tmp = boot::boot(df_tmp,boot_tmp_f1,N)
  ret$precisionLenientBoot = boot_tmp
  cis_tmp = boot::boot.ci(boot_tmp,conf=c(0.90,0.95,0.99),,type="all")
  ret$precisionLenientCI90l = cis_tmp$basic[1,4]
  ret$precisionLenientCI90u = cis_tmp$basic[1,5]
  ret$precisionLenientCI95l = cis_tmp$basic[2,4]
  ret$precisionLenientCI95u = cis_tmp$basic[2,5]
  ret$precisionLenientCI99l = cis_tmp$basic[3,4]
  ret$precisionLenientCI99u = cis_tmp$basic[3,5]

  # f strict
  cat(" Fs")
  df_tmp = data.frame(nt=bydocs$targets,nr=bydocs$responses,ncs=bydocs$correctStrict)
  boot_tmp = boot::boot(df_tmp,boot_tmp_f2,N)
  ret$F1StrictBoot = boot_tmp
  cis_tmp = boot::boot.ci(boot_tmp,conf=c(0.90,0.95,0.99),,type="all")
  ret$F1StrictCI90l = cis_tmp$basic[1,4]
  ret$F1StrictCI90u = cis_tmp$basic[1,5]
  ret$F1StrictCI95l = cis_tmp$basic[2,4]
  ret$F1StrictCI95u = cis_tmp$basic[2,5]
  ret$F1StrictCI99l = cis_tmp$basic[3,4]
  ret$F1StrictCI99u = cis_tmp$basic[3,5]

  # f lenient
  cat(" Fl")
  df_tmp = data.frame(nt=bydocs$targets,nr=bydocs$responses,ncs=(bydocs$correctPartial+bydocs$correctStrict))
  boot_tmp = boot::boot(df_tmp,boot_tmp_f2,N)
  ret$F1LenientBoot = boot_tmp
  cis_tmp = boot::boot.ci(boot_tmp,conf=c(0.90,0.95,0.99),,type="all")
  ret$F1LenientCI90l = cis_tmp$basic[1,4]
  ret$F1LenientCI90u = cis_tmp$basic[1,5]
  ret$F1LenientCI95l = cis_tmp$basic[2,4]
  ret$F1LenientCI95u = cis_tmp$basic[2,5]
  ret$F1LenientCI99l = cis_tmp$basic[3,4]
  ret$F1LenientCI99u = cis_tmp$basic[3,5]

  options(oldopts)
  cat(" DONE.\n")
  return(ret)
}


boot_tmp_f1 = function(d,w) { s=sum(d$n[w]); if(s==0) { return(0.0) } else { return(sum(d$ncs[w])/s) } }

boot_tmp_f2 <- function(d,w) {
  s=sum(d$nt[w]); if(s==0) { r=0.0 } else { r=sum(d$ncs[w])/s };
  s=sum(d$nr[w]); if(s==0) { p=0.0 } else { p=sum(d$ncs[w])/s };
  if(p+r==0.0) { return(0.0) } else { return( (2.0*p*r)/(p+r)  ) }
}

