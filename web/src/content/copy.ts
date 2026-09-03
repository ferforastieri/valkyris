export type Locale = 'pt-BR' | 'en';

export const copy = {
  'pt-BR': {
    metaTitle: 'Valkyris — sua casa avisa, seus dados ficam',
    metaDescription: 'Monitoramento local-first para câmeras ONVIF, com detecção local, clipes e alertas Android.',
    nav: ['Visão geral', 'Como funciona', 'Instalação', 'Compatibilidade'],
    heroEyebrow: 'Monitoramento residencial local-first',
    heroTitle: 'Sua casa avisa.\nSeus dados ficam.',
    heroBody: 'Conecte câmeras ONVIF, reconheça sons e movimentos dentro de casa e receba o momento certo no Android — sem enviar sua rotina para uma nuvem central.',
    install: 'Instalar com Docker',
    github: 'Ver no GitHub',
    live: 'AO VIVO',
    camera: 'Entrada principal',
    status: 'Proteção ativa',
    alert: 'Choro detectado',
    alertMeta: 'Agora · 91% de confiança',
    principlesTitle: 'Privacidade não é uma opção nas configurações.',
    principlesBody: 'É a arquitetura. A câmera conversa com uma máquina na sua rede. O celular conversa com ela por LAN ou VPN. O vídeo não precisa atravessar um servidor nosso.',
    principles: [
      ['Dentro de casa', 'Vídeo, áudio, regras e clipes são processados e armazenados na sua instalação.'],
      ['Você decide o acesso', 'O primeiro celular cria a senha da casa; depois, o administrador autoriza outros por convite temporário.'],
      ['Compatível por padrão', 'ONVIF Profile S e RTSP evitam prender sua casa a uma única marca.']
    ],
    detectTitle: 'Sinais que importam.\nRuído que fica de fora.',
    detectBody: 'Combine confiança mínima, confirmações, horários e intervalo entre alertas para reduzir falsos positivos.',
    detectors: ['Movimento', 'Pessoa', 'Choro', 'Grito', 'Vidro quebrando', 'Alarme de fumaça', 'Sirene', 'Campainha', 'Batida', 'Latido'],
    flowTitle: 'Da lente ao seu celular',
    flow: [
      ['Câmera', 'RTSP fornece imagem e áudio; ONVIF anuncia eventos e controles disponíveis.'],
      ['Valkyris', 'Regras locais confirmam o sinal e preservam 5 s antes + 10 s depois.'],
      ['Android', 'Uma notificação persistente ou alarme abre diretamente o evento e o live view.']
    ],
    appTitle: 'Uma interface calma.\nAté algo pedir atenção.',
    appBody: 'Ao vivo, movimentos PTZ, timeline, clipes e regras em um app Kotlin nativo com tema claro e escuro.',
    setupTitle: 'Comece em três passos',
    steps: [
      ['Prepare a câmera', 'Conecte-a ao Wi-Fi e crie a Camera Account RTSP/ONVIF. Na TC40, o app Tapo só é necessário nesta etapa.'],
      ['Suba o Valkyris', 'Execute o instalador em um Linux com Docker que alcance a mesma rede da câmera.'],
      ['Entre pelo Android', 'Informe seu endereço HTTPS e crie a senha da casa no primeiro acesso. Convites para outros celulares nascem dentro do app.']
    ],
    compatibilityTitle: 'Compatibilidade honesta',
    compatibilityBody: 'Câmeras ONVIF Profile S com RTSP funcionam por capacidades: PTZ, áudio, snapshots e eventos só aparecem quando o dispositivo os anuncia. A TC40 oferece vídeo, áudio, eventos e PTZ; áudio bidirecional, sirene e holofote proprietários ficam fora.',
    faqTitle: 'Perguntas frequentes',
    faq: [
      ['Preciso abrir portas no roteador?', 'Não. Use LAN em casa e uma VPN como Tailscale ou WireGuard fora dela.'],
      ['O Valkyris grava o tempo todo?', 'A mídia mantém apenas um buffer curto. Um clipe permanente só nasce quando uma regra dispara.'],
      ['A detecção acerta sempre?', 'Nenhum classificador acerta sempre. Por isso cada regra combina confiança, confirmações e cooldown.'],
      ['Posso usar outra câmera?', 'Sim, desde que ofereça ONVIF Profile S e RTSP. Recursos variam conforme as capacidades anunciadas.']
    ],
    footer: 'Software independente, aberto e feito para redes privadas.',
    disclaimer: 'Valkyris não é afiliado à TP-Link ou Tapo.'
  },
  en: {
    metaTitle: 'Valkyris — your home speaks, your data stays',
    metaDescription: 'Local-first ONVIF camera monitoring with on-premise detection, event clips and Android alerts.',
    nav: ['Overview', 'How it works', 'Install', 'Compatibility'],
    heroEyebrow: 'Local-first home monitoring',
    heroTitle: 'Your home speaks.\nYour data stays.',
    heroBody: 'Connect ONVIF cameras, recognize sounds and movement at home, and receive the right moment on Android — without sending your routine to a central cloud.',
    install: 'Install with Docker', github: 'View on GitHub', live: 'LIVE', camera: 'Front entrance', status: 'Protection active', alert: 'Baby cry detected', alertMeta: 'Now · 91% confidence',
    principlesTitle: 'Privacy is not a settings toggle.', principlesBody: 'It is the architecture. The camera talks to a machine on your network. Your phone reaches it over LAN or VPN. Video never needs to cross our servers.',
    principles: [['Inside your home','Video, audio, rules and clips are processed and stored by your installation.'],['You grant access','The first phone creates the home password; the administrator then authorizes others with temporary invitations.'],['Compatible by default','ONVIF Profile S and RTSP keep your home independent from any single brand.']],
    detectTitle: 'Signals that matter.\nNoise that stays out.', detectBody: 'Combine minimum confidence, confirmations, schedules and cooldowns to reduce false positives.', detectors: ['Motion','Person','Baby cry','Scream','Glass breaking','Smoke alarm','Siren','Doorbell','Knock','Dog bark'],
    flowTitle: 'From the lens to your phone', flow: [['Camera','RTSP supplies video and audio; ONVIF advertises available events and controls.'],['Valkyris','Local rules confirm the signal and preserve 5 s before + 10 s after.'],['Android','A persistent notification or alarm opens the event and live view directly.']],
    appTitle: 'A calm interface.\nUntil something needs attention.', appBody: 'Live view, PTZ motion, timeline, clips and rules in a native Kotlin app with light and dark themes.',
    setupTitle: 'Start in three steps', steps: [['Prepare the camera','Connect it to Wi-Fi and create the RTSP/ONVIF camera account. For TC40, Tapo is only needed at this stage.'],['Run Valkyris','Execute the installer on a Docker Linux host that can reach the camera network.'],['Sign in on Android','Enter your HTTPS address and create the home password on first access. Invitations for other phones are generated inside the app.']],
    compatibilityTitle: 'Honest compatibility', compatibilityBody: 'ONVIF Profile S cameras with RTSP work by capability: PTZ, audio, snapshots and events only appear when advertised. TC40 offers video, audio, events and PTZ; proprietary two-way audio, siren and spotlight are out of scope.',
    faqTitle: 'Frequently asked questions', faq: [['Must I open router ports?','No. Use LAN at home and a VPN such as Tailscale or WireGuard away from it.'],['Does Valkyris record continuously?','Media keeps only a short buffer. A permanent clip is created only when a rule fires.'],['Is detection always correct?','No classifier is perfect. Every rule can combine confidence, confirmations and cooldown.'],['Can I use another camera?','Yes, if it provides ONVIF Profile S and RTSP. Features depend on advertised capabilities.']],
    footer: 'Independent open software made for private networks.', disclaimer: 'Valkyris is not affiliated with TP-Link or Tapo.'
  }
} as const;
