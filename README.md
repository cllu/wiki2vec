# Wiki2Vec

Utilities for creating Word2Vec vectors for Dbpedia Entities via a Wikipedia Dump.

Within the release of [Word2Vec](http://code.google.com/p/word2vec/) the Google team released vectors for freebase entities trained on the Wikipedia. These vectors are useful for a variety of tasks.

This Tool will allow you to generate those vectors. Instead of `mids` entities will be addressed via `DbpediaIds` which correspond to wikipedia article's titles.
Vectors are generated for (i) words appearing inside wikipedia (ii) vectors for topics i.e: `dbpedia/Barack_Obama`.

 
## Setup

- Requires Java, sbt
- Run `sbt assembly` to create a fat-jar `target/scala-2.10/wiki2vec-assembly-1.0.jar`
- Download Wikipedia article dump `multistreaming-xml.bz2`, put it under `data` folder
- Run the following to create a file with one line per article:

    java -Xmx10G -Xms10G -cp wiki2vec-assembly-1.0.jar org.idio.wikipedia.dumps.CreateReadableWiki ./enwiki-20150205-pages-articles-multistream.xml.bz2 corpus
    
- Run the following to process the generated file using Spark, this will stem and tokenize article contents

    ./spark-1.2.0-bin-hadoop2.4/bin/spark-submit --class "org.idio.wikipedia.word2vec.Word2VecCorpus" wiki2vec-assembly-1.0.jar /gds/cllu/workspace/wiki2vec/ReadableWiki fakeRedirectFile /gds/cllu/workspace/wiki2vec/corpus
    
- Run `cat corpus/part* > enwiki-20150205.corpus` to generate a single corpus file
The generated corpus file is around 21G.

Then you can feed the corpus file to standard word2vec program.

### Readable Wikipedia

Wikipedia dumps are stored in xml format. This is a difficult format to process in parallel because the  xml file has to be streamed getting the articles on the go.
A Readable wikipedia Dump is a transformation of the dump such that it is easy to pipeline into tools such as Spark or Hadoop.

Every line in a readable wikipedia dump follows the format:
`Dbpedia Title` `<tab>` `Article's Text`

The class `org.idio.wikipedia.dumps.ReadableWiki` gets a `multistreaming-xml.bz2`wikipedia dump and outputs a readable wikipedia.

params:
 - path to wikipedia dump
 - path to output readable wikipedia
i.e:

`java -Xmx10G -Xms10G -cp org.idio.wikipedia.dumps.ReadableWiki wiki2vec-assembly-1.0.jar path-to-wiki-dump/eswiki-20150105-pages-articles-multistream.xml.bz2 pathTo/output/ReadableWikipedia`


### Word2Vec Corpus

Creates a Tokenized corpus which can be fed into tools such as Gensim to create Word2Vec vectors for Dbpedia entities.

- Every Wikipedia link to an article within wiki is replaced by : `DbpediaId/DbpediaIDToLink`. i.e: 

if an article's text contains: 
```
[[ Barack Obama | B.O ]] is the president of [[USA]]
```

is transformed into:

```
DbpediaID/Barack_Obama B.O is the president of DbpediaID/USA
```

- Articles are tokenized (At the moment in a very naive way)


#### Getting a Word2Vec Corpus

1. Make sure you got a `Readable Wikipedia`
2. Download Spark : http://d3kbcqa49mib13.cloudfront.net/spark-1.2.0-bin-hadoop2.4.tgz
3. In your Spark folder do:
  ```
  bin/spark-submit --class "org.idio.wikipedia.word2vec.Word2VecCorpus"  target/scala-2.10/wiki2vec-assembly-1.0.jar   /PathToYourReadableWiki/readableWiki.lines /Path/To/RedirectsFile /PathToOut/Word2vecReadyWikipediaCorpus
  ```
4. Feed your corpus to a word2vec tool

### Stemming

By default the word2vec corpus is always stemmed. If you don't want that to happen: 

#### If using the automated scripts..
pass None as an extra argument

`sudo sh prepare.sh es_ES /mnt/data/ None`  will work on the spanish wikipedia and won't stem words

#### If you are manually running the tools:
Pass None as an extra argument when calling spark
 ```
 bin/spark-submit --class "org.idio.wikipedia.word2vec.Word2VecCorpus"  target/scala-2.10/wiki2vec-assembly-1.0.jar   /PathToYourReadableWiki/readableWiki.lines /Path/To/RedirectsFile /PathToOut/Word2vecReadyWikipediaCorpus None
 ```

## ToDo:
- Remove hard coded spark params
- Handle Wikipedia Redirections
- Intra Article co-reference resolution
