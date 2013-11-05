module namespace web = 'cost.cloud.web';

import module namespace fees = 'cost.cloud.Fees';

declare updating function web:NodeDirty($file as xs:string, $node){
  let $id := xs:string($node/costId/text())
  let $depth := xs:int($node/depth/text())
  return db:output(fees:addjob($file, $id, $depth))
};

declare 
%rest:path("/cost") %rest:GET
%rest:query-param("file", "{$file}")
%rest:query-param("id", "{$id}")
function web:GetNode($file as xs:string, $id as xs:string){
  doc($file)//cost[costId=$id]
};

declare 
%rest:path("/cost") %rest:POST  %output:method("json")
%rest:form-param("type","{$type}")
%rest:form-param("node","{$node}")
%rest:form-param("file","{$file}")
%rest:form-param("parentid","{$parentid}")
updating function web:InsertNode($type as xs:string, $node as xs:string*, $file as xs:string*, $parentid as xs:string*){
  let $id := random:uuid()
  let $parent := doc($file)//cost[costId=$parentid]
  let $n := 
    <cost>
		  <fees>{doc('fees')//node()[name()=$type]/node()}</fees>
		  <costType>{$type}</costType>
		  <costId>{$id}</costId>
      <depth>{if($parent) then 1+$parent/depth else 1}</depth>
		  {if($node)then parse-xml($node)/*/node() else ()}
    </cost>
  return if($parent) then(     
      insert node $n into $parent, 
      web:NodeDirty($file, $n),
      db:output(<json type="object"><id>{$id}</id></json>)    
  )
  else(
    let $fileid:= random:uuid() return(     
      db:create($fileid, $n, $fileid), 
      web:NodeDirty($fileid, $n),
      db:output(<json type="object"><file>{$fileid}</file><id>{$id}</id></json>)
    )
  )    
};

declare
%rest:path("/cost")  %rest:PUT
%rest:form-param("file","{$file}")
%rest:form-param("id","{$id}")
%rest:form-param("name","{$name}")
%rest:form-param("value","{$value}") 
updating function web:UpdateNode($file as xs:string, $id as xs:string, $name as xs:string, $value as xs:string){
  let $n := doc($file)//cost[costId=$id] return
  if($n) then(
    let $target := $n/node()[name()=$name] return
    if($target) then (
       replace value of node $target with $value,
       web:NodeDirty($file, $n)
    ) else ()   
  ) else ()
};

declare
%rest:path("/cost")  %rest:DELETE
%rest:form-param("file","{$file}")
%rest:form-param("id","{$id}") 
updating function web:DeleteNode($file as xs:string, $id as xs:string){
  let $n := doc($file)//cost[costId=$id] return
  if ($n) then(
    delete node $n,
    web:NodeDirty($file, $n/parent::node())
  ) else ()
};
