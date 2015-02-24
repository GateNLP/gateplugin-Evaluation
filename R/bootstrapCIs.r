library(boot)

## TODO: also calculate the CIs for the lenient measures.

args=commandArgs(trailingOnly=TRUE)

# the number of bootstrap samples
N=10000

filename=args[1]

if(is.na(filename)) {
  stop("Need the path to the EvaluationTagging tsv file")
}

## Read the file into a data frame
d=read.delim(filename,header=TRUE)

## remove the rows for the document summaries
## TODO: we should replace this to remove all rows which mach \[doc:all:.*\]
d2=d[d$docName!="[doc:all:micro]",]

## get the necessary columns and create data frames for the bootstrapping
nt=d2$targets
ncs=d2$correctStrict
nr=d2$responses
r=data.frame(n=nt,ncs=ncs)
p=data.frame(n=nr,ncs=ncs)
f=data.frame(nt=nt,nr=nr,ncs=ncs)

## the function for calculating the precision/recall statistics during the sampling
f1 <- function(d,w) { s=sum(d$n[w]); if(s==0) { return(0.0) } else { return(sum(d$ncs[w])/s) } }

## the function for calculating the f1.0 measure during sampling
f2 <- function(d,w) {
  s=sum(d$nt[w]); if(s==0) { r=0.0 } else { r=sum(d$ncs[w])/s };
  s=sum(d$nr[w]); if(s==0) { p=0.0 } else { p=sum(d$ncs[w])/s };
  if(p+r==0.0) { return(0.0) } else { return( (2.0*p*r)/(p+r)  ) }
}

## do the bootstrapping for all three measures

br=boot(r,f1,N)
bp=boot(p,f1,N)
bf=boot(f,f2,N)

## get the BS estimates for the confidence intervals for 0.9, 0.95 amd 0.99

## turn off the warnings we get because we do not return our own standard deviations
## for the studentized interval estimates
options(warn=-1)
cir=boot.ci(br,conf=c(0.90,0.95,0.99))
cip=boot.ci(bp,conf=c(0.90,0.95,0.99))
cif=boot.ci(bf,conf=c(0.90,0.95,0.99))
options(warn=0)

## print them out
## TODO: use our own print function to do that a bit prettier
cat("PRECISION:\n")
print(cip)
cat("RECALL:\n")
print(cir)
cat("F1.0-MEASURE:\n")
print(cif)



