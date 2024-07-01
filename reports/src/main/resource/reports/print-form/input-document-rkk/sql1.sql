with idrkk as (
	SELECT Item 
	FROM QR_Id_List WHERE Request = $request_id
)

SELECT
	COALESCE(CONCAT (
		rkk.regnumprist,
		rkk.regnumcnt,
		rkk.regnumfin
	), '') AS regNumber,
	COALESCE(to_char(rkk.regdate, 'DD.MM.YYYY'), '') as regdate, --Дата регистрации
	COALESCE(rkkBase.type, '') as docType, --Вид документа
	COALESCE(inputRkk.foreignnumber, '') as docNmber, --Номер документа
	COALESCE(to_char(inputRkk.foreignregdate, 'DD.MM.YYYY'), '') as docDate, --дата документа
	--Тематика документа
	COALESCE((select string_agg(theme.Theme, ', ') 
		from F_DP_RkkBase_Theme theme 
		where theme.owner = inputRkk.id)
	, '') AS theme,
	COALESCE(regPlace.orig_Shortname, '') as regPlace,
	COALESCE(rkkBase.subject, '') as subject,
	COALESCE(to_char(rkk.ctrldeadline, 'DD.MM.YYYY'), '') as ctrldeadline, --Срок исполнения
	inputRkk.id::text as docid,
	inputRkk.id_type::text as docidtype,
	module.replica || ':' || n2p.nunid as docunid,

	--correspondent
	case when corrName is not null then corrName
		when authorName is not null then authorName
		else 'Не указан'
	end as corrName,
	case when corr.orig_shortName is null then null
	  else authorName.authorName end as authorName,

	--Addressee
	COALESCE((select string_agg(addrb.orig_shortname,', ')
		from F_DP_InputRkk_Addressee addr
		JOIN So_Beard addrb on addrb.id = addr.addressee
		where addr.owner = inputRkk.id)
	,'') as addressees,

	--Статус исполнения
	COALESCE(person.lastname || ' ' || left(person.firstname, 1) || '.' || left(person.middlename, 1) || '.', '') as whoexec,
	COALESCE(wrapper.rkkExecutionStatus, '') as rkkExecutionStatus,
	COALESCE(to_char(wrapper.ctrldateexecution, 'DD.MM.YYYY'), '') as ctrldateexecution,
	--COALESCE(resolutionsstate, '') as control
	COALESCE(rkk.rkkctrlstate::text, '') as control
FROM idrkk
join F_DP_InputRkk inputRkk on inputRkk.id in (idrkk.Item)
JOIN F_DP_Rkk rkk on rkk.id = inputRkk.id
JOIN F_DP_RkkBase rkkBase on rkkBase.id = inputRkk.id

JOIN So_Beard regPlace on regPlace.id = rkkBase.RegCode

--correspondent
left join so_beard corr on corr.id = inputrkk.fromid

--Статус исполнения
left join lateral(select rkk.ctrlwhoexecpid as id,
	CASE
	 	WHEN (rkk.CtrlDateExecution IS NOT NULL) THEN 'Исполнен'
	 	WHEN (rkk.CtrlDeadline IS NOT NULL and Date(rkk.CtrlDeadline) < Date(now())) THEN 'Просрочен'
	ELSE 'На исполнении'
	END AS rkkExecutionStatus, rkk.ctrldateexecution
) wrapper on 1=1

left join lateral(select string_agg(inprkk_ap.authorplain,', ') as authorName
	from f_dp_inputrkk_authorplain inprkk_ap
	where inprkk_ap.owner = inputRkk.id
)authorName on 1=1

left join lateral(select
	case when corr.orig_shortName is not null then corr.orig_shortname
		when authorName.authorName is not null then authorName.authorName
		else 'Не указан'
	end as corrName
)corrName on 1=1

LEFT JOIN SO_Person person on person.id = to_number(coalesce(substring(NULLIF(wrapper.id,''),5,12),null),'999999999999') 
	OR (person.migrationid like '%' || wrapper.id || '%' and wrapper.id <> '')

JOIN nunid2punid_map n2p on cast(substring(n2p.punid, 1, 4) as int) = rkk.id_type and cast(substring(n2p.punid, 5, 12) as int) = rkk.id
JOIN ss_module module ON module.id = rkkbase.module