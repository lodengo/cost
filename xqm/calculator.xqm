module namespace c = 'cost.cloud.calculator';
import module namespace fees = 'cost.cloud.Fees';

declare updating function c:calcs($file as xs:string, $ids as xs:string){  
  let $ids := tokenize($ids, ",") 
  for $id in $ids return c:calc($file, $id)
};

declare updating function c:calc($file as xs:string, $id as xs:string){
   let $u :=  doc($file)//cost[costId=$id] return
   if($u) then(
     let $n := copy $c := $u/fees
		   modify(
		    for $fee in $c//fee return
		    replace node $fee/feeExpr with 
		      xquery:eval(concat("import module namespace f = 'cost.cloud.calculator.func';", serialize($fee/feeExpr)), map{'$f' := $fee, '$c':=$u})   
		   )
		   return $c
		 return try{
			 let $result := parse-xml(fees:calculate(serialize($n))) return (
			   for $fee in $result//fee return replace value of node $u/fees//fee[feeName=$fee/feeName/text()]/feeResult with $fee/feeResult/text(), 
			   let $parentid := $u/../costId/text() return 
			     if($parentid) then db:output(concat($parentid, '')) else ()       
			 )
		 } catch * {()}	 
   )
   else ()	     
};