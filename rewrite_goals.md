# Goals

* simplify the parsing of layout configs
* go to final parsed shape directly 
* use transducers and enable a generic pattern for transformations
  * datastructure to contain both the config and the to-be-built parsed structure
* figure out a functional way of not having to build a secondary structure based 
  on the actual number of columns in in-data, we should be agnostic
* support fixed width columns together with global width 
* it seems that we will need a phase one traversal of the in-data where: 
  * we figure out the number of columns
  * we figure out the max col widths
* support providing cell data transform function which gets: 
  * cell data, row, col, col-width
* if using fixed width columns, specify 
* should we create a "cell descriptor" when parsing the layout? This 
  descriptor would contain everything needed to render a cell

* simplify this shit