module namespace c = 'cost.cloud.calculator';
import module namespace fees = 'cost.cloud.Fees';

declare updating function c:calcs($db as xs:string, $ids as xs:string){  
  let $ids := tokenize($ids, ",") 
  for $id in $ids return c:calc($db, $id)
};

declare updating function c:calc($db as xs:string, $id as xs:string){
   let $u :=  doc($db)//组价单元[单元id=$id]  
	 let $n := copy $c := $u/组价汇总
	   modify(
	    for $fee in $c//费用 return
	    replace node $fee/计算公式 with 
	      xquery:eval(concat("import module namespace f = 'cost.cloud.calculator.func';", serialize($fee/计算公式)), map{'$f' := $fee, '$u':=$u})   
	   )
	   return $c
	 let $result := parse-xml(fees:calculate(serialize($n))) return (
	   for $fee in $result//费用 return replace value of node $u/组价汇总//费用[费用名称=$fee/费用名称/text()]/计算结果 with $fee/计算结果/text(), 
	   let $parentid := $u/../单元id/text()
	   let $depth := $u/../depth/text() return 
	     if($parentid) then db:output(concat($parentid, '')) else ()       
	 )     
};