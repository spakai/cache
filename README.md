# cache
Cache for multi lookups

# Packages

Cache -  if we have multiple looks to different indexes i.e composite indexes , we can use a cache to store the most used             calls.

Parser  - there is one type implemented
          i) CSV to read comma separated data from files

Indexes - there are 4 types of indexes implemented
          i) PrimaryTreeIndex with unique keys for exact and best match
          ii) SecondaryTreeIndex with duplicate keys for exact and best match 
          iii) PrimaryHashIndex with unique keys for exact match
          iv) SecondaryHashIndex with duplicate keys for exact match
