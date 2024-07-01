select to_char(fdrd.olddate, 'DD.MM.YYYY') as olddatetable,
case when fdrd.docreason = '-' then null 
	else SPLIT_PART(fdrd.docreason, '%', 3) || ':' ||SPLIT_PART(fdrd.docreason, '%', 2) end as unid,
fdrd.deferreason as transfertable
from F_DP_InputRkk inputrkk
join f_dp_rkkworegandctrl_ddef fdrd on fdrd.owner = inputrkk.id
where inputrkk.id IN ($id)