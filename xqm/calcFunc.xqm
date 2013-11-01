module namespace cf = 'cost.cloud.calculator.func';


declare function cf:u($u, $e){
  number($u/node()[name()=$e]/text())
};

declare function cf:nm($ns, $e){
  if($ns/fees/fee[feeName=$e]) then  sum($ns/fees/fee[feeName=$e]/feeResult) 
  else  sum($ns//node()[name()=$e])
};

declare function cf:nm($ns, $e1, $e2){
  sum(for $n in $ns return $n/node()[name()=$e1] * $n/node()[name()=$e2])
};