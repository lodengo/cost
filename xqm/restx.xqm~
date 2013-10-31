module namespace web = 'cost.cloud.web';

import module namespace fees = 'cost.cloud.Fees';

declare updating function web:NodeDirty($db as xs:string, $node){
  let $id := xs:string($node/单元id/text())
  let $depth := xs:int($node/depth/text())
  return db:output(fees:addjob($db, $id, $depth))
};

declare 
%rest:path("/node") %rest:POST  %output:method("json")
%rest:form-param("type","{$type}")
%rest:form-param("node","{$node}")
%rest:form-param("db","{$db}")
%rest:form-param("parentid","{$parentid}")
updating function web:InsertNode($type as xs:string, $node as xs:string*, $db as xs:string*, $parentid as xs:string*){
  let $id := random:uuid()
  let $parent := doc($db)//组价单元[单元id=$parentid]
  let $n := 
    <组价单元>
		  <组价汇总>{doc('template')/组价汇总/node()[name()=$type]/node()}</组价汇总>
		  <单元类型>{$type}</单元类型>
		  <单元id>{$id}</单元id>
      <depth>{if($parent) then 1+$parent/depth else 1}</depth>
		  {if($node)then parse-xml($node) else ()}
    </组价单元>
  return if($parent) then(     
      insert node $n into $parent, 
      web:NodeDirty($db, $n),
      db:output(<json type="object"><id>{$id}</id></json>)    
  )
  else(
    let $dbid:= random:uuid() return(     
      db:create($dbid, $n, $dbid), 
      web:NodeDirty($dbid, $n),
      db:output(<json type="object"><db>{$dbid}</db><id>{$id}</id></json>)
    )
  )    
};
