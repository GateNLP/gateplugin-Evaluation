library(boot)

## TODO: make sure the input file is for one evaluation only
## TODO: once we made sure, get the evaluation id and use it to label the 
## plot and also set the output file name for the pdf. Alternately, we could
## allow command line parameters for these things

args=commandArgs(trailingOnly=TRUE)

filename=args[1]

if(is.na(filename)) {
  stop("Need the path to the EvaluationTagging tsv file")
}

## Read the file into a data frame
d=read.delim(filename,header=TRUE)

## remove the rows which are not for the  document micro summaries
d2=d[d$docName=="[doc:all:micro]",]

## get the necessary columns 
th=d2$threshold
ps=d2$precisionStrict
rs=d2$recallStrict
fs=d2$F1Strict
pl=d2$precisionLenient
rl=d2$recallLenient
fl=d2$F1Lenient

## TODO: save this as PDF: 
## open pdf device: pdf() .. plot() .. dev.off()

plot(rs,ps,xlab="Recall",ylab="Precision")
lines(rs,ps,type="l")
## TODO: restrict the number of thresholds to show
## TODO: also show F? 
text(rs,ps,labels=th,cex=0.7,pos=3)

## TODO: can we put both strict and lenient into the same plot?
## TODO: add text showing the th/maxF strict and lenient

