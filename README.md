# Lumin Blades — Forge 1.20.1

14 espadas mágicas luminosas com shaders GLSL próprios, Bloom via `PostChain`, partículas aditivas, rastros, aura procedural e distorção de espaço. Estilo inspirado em Arcane Vortex; todo o código e todos os assets são originais.

> **Estado:** o código foi escrito sem acesso ao Forge/Gradle (o ambiente de geração não tem rede), então **não foi compilado nem testado em jogo**. Espere pequenos ajustes de API/mappings na primeira compilação. Os passos de validação estão abaixo.

## Arquitetura (onde fica cada coisa)

| Responsabilidade | Local |
|---|---|
| Entrada / registros | `LuminBlades`, `registry/` (itens, partículas, aba criativa) |
| Catálogo das 14 espadas | `sword/SwordType` (id, cor, parâmetros padrão, habilidade) |
| Item genérico | `sword/LuminSwordItem` (botão direito = habilidade, golpe = `onHit`) |
| Habilidades (servidor) | `ability/Abilities`, utilitários em `ability/Fx`, efeitos por tick em `ability/ServerScheduler` |
| Config | `config/LuminConfig` → `config/luminblades-common.toml` |
| Rede | `network/` (`DistortionPacket`: servidor pede distorção de espaço ao cliente) |
| Partículas | `vfx/GlowParticleOptions` (comum), `vfx/GlowParticle` (cliente, aditiva, com limite) |
| Rastros / impactos | `vfx/VfxManager` (buffers em anel reutilizados), `client/render/TrailRenderer` |
| Aura (shader core) | `client/render/AuraRenderer` + `assets/luminblades/shaders/core/blade_aura.*` |
| Bloom + distorção (PostChain) | `client/render/BloomPostProcessor` + `assets/luminblades/shaders/post/lumin_bloom.json` + `shaders/program/*` |
| Animações | `client/anim/SwordAnimator` (estado/easing) + `RenderHandEvent` em `client/ClientEvents` |
| Eventos de cliente | `client/ClientEvents` (jogo) e `client/ClientModEvents` (registro) |

### Decisões
- **Mappings oficiais Mojang** + Forge 47.x: estáveis para 1.20.1.
- **PostChain próprio** (não `GameRenderer.loadEffect`): controle total de quando roda; só executa se há espada na mão, espada de outro jogador visível ou impacto ativo.
- **Bloom roda em `RenderGuiEvent.Pre`**: nesse ponto o mundo *e a mão* já estão no framebuffer, então a lâmina em primeira pessoa também brilha. Limitação: com F1 (GUI oculta) o Bloom não roda; a aura continua.
- **Aura em shader core** (`blade_aura`) com ruído/redemoinho animado por `Time`; o Bloom amplifica o brilho.
- **Distorção**: o servidor envia `DistortionPacket`; o cliente projeta o ponto em UV e o `distort.fsh` faz onda em anel + aberração cromática (1 impacto por frame, o mais forte).
- **Performance**: culling por distância e frustum (aura/rastro), rastros em anel de tamanho fixo, limite global de partículas (`maxParticles`), partículas em lote (1 pacote por explosão), Bloom só quando necessário.
- **Config COMMON**: em servidor dedicado, cada cliente usa a própria cópia dos valores visuais.

## As 14 espadas

Ativação = ao empunhar (aura sobe com easing; cai a ~45% na recarga). Parâmetros padrão: `intensity / duração(ticks) / size / recarga(ticks) / power`.

| # | Espada (id) | Habilidade (botão direito) | Visual de ativação | Visual de ataque (golpe) | Padrões |
|---|---|---|---|---|---|
| 1 | Aurora Vortex (`aurora_vortex`) | Vórtice que puxa inimigos e implode | Anel convergente + espirais | Faíscas ciano | 1.0 / 50 / 6.0 / 240 / 8 |
| 2 | Falling Star (`falling_star`) | Investida que fere quem atravessa | Rastro de estrelas | Faíscas douradas | 1.0 / 6 / 8.0 / 120 / 7 |
| 3 | Prismatic Edge (`prismatic_edge`) | 3 feixes que refratam | Feixes em arco-íris | Faíscas brancas | 1.0 / 1 / 12.0 / 160 / 9 |
| 4 | Lunar Tide (`lunar_tide`) | Onda em arco que empurra | Crescente expansivo | Faíscas azul-prata | 1.0 / 14 / 9.0 / 140 / 7 |
| 5 | Crimson Nova (`crimson_nova`) | Carga + supernova em área | Partículas convergem, 3 anéis, distorção | Faíscas rubras | 1.2 / 30 / 6.0 / 400 / 14 |
| 6 | Ethereal Veil (`ethereal_veil`) | Teletransporte curto | Linha + explosões + distorção | Faíscas violeta | 1.0 / 1 / 10.0 / 100 / 0 |
| 7 | Solar Flame (`solar_flame`) | Leque flamejante | Varredura de chamas | Incendeia o alvo | 1.0 / 16 / 7.0 / 120 / 8 |
| 8 | Glimmer Frost (`glimmer_frost`) | Nuvem de cristais que congela | Anel gélido + neve luminosa | Lentidão + gelo | 1.0 / 100 / 5.0 / 300 / 4 |
| 9 | Arcane Tempest (`arcane_tempest`) | Raio em cadeia (até 5 alvos) | Raios em zigue-zague | Faíscas azuis | 1.2 / 12 / 14.0 / 160 / 7 |
| 10 | Luminous Root (`luminous_root`) | Raízes de luz prendem alvos | Vinhas luminosas subindo | Faíscas verdes | 1.0 / 80 / 4.5 / 260 / 3 |
| 11 | Eclipse (`eclipse`) | Área de sombra (cegueira/fraqueza) | Corona dourada + núcleo escuro | Aplica fraqueza | 1.0 / 120 / 5.5 / 500 / 3 |
| 12 | Wandering Comet (`wandering_comet`) | Projétil teleguiado em espiral | Espiral + explosão | Faíscas rosa | 1.0 / 50 / 4.0 / 140 / 12 |
| 13 | Cosmic Pulse (`cosmic_pulse`) | 3 ondas de choque | Anéis + distorção do espaço | Faíscas magenta | 1.2 / 36 / 9.0 / 320 / 9 |
| 14 | Radiant Phoenix (`radiant_phoenix`) | Cura + queima em área | Asas de luz que batem | Cura 1 ponto ao golpear | 1.0 / 40 / 5.0 / 600 / 10 |

### Parâmetros configuráveis
Em `config/luminblades-common.toml`:
- **Por espada** (`[aurora_vortex]` etc.): `color`, `intensity`, `durationTicks`, `size`, `particleDensity`, `cooldownTicks`, `power`.
- **Globais** (`[global]`): `bloomEnabled/Threshold/Intensity/BlurRadius`, `auraEnabled/Size`, `trailEnabled/LifetimeTicks/Width`, `distortionEnabled/Strength`, `maxParticles`, `cullDistance`.


## Efeitos cinematográficos (v2)

Inspirados nas imagens de referência (corte cósmico, círculo rúnico, olho de dragão, sol, fitas arco-íris, buraco negro). O servidor escolhe o efeito (`Fx.cine` / `Fx.cineSelf`) e envia um `CinematicPacket`; o cliente renderiza (`client/cine/`).

| Efeito | Técnica | Arquivos |
|---|---|---|
| **Buraco negro** | Pós-processamento: lente gravitacional + horizonte + anel de fótons + disco de acreção inclinado com redemoinho e efeito Doppler | `program/distort.fsh` |
| **Corte cósmico** | Faixa 3D (sempre voltada à câmera) com nebulosa fbm, estrelas cintilantes, bordas brancas e estilhaços quadrados | `core/galaxy_slash.*` |
| **Círculo rúnico** | Quad procedural (chão ou vertical): anéis, 2 faixas de glifos girando em sentidos opostos, hexagrama, marcas | `core/rune_circle.*` |
| **Olho de dragão** (só o portador) | Overlay em tela cheia: pálpebras, íris com veias, pupila em fenda, raios de velocidade, vinheta | `program/cine_overlay.fsh` |
| **Sol** (só o portador) | Overlay em tela cheia: disco granulado, corona, raios, clarão | `program/cine_overlay.fsh` |
| **Fitas arco-íris** | 5 arcos em cúpula + anel no chão (matiz percorre a fita) + confete triangular | `core/rainbow_ribbon.*` |

Mapeamento por espada:
- Aurora Vortex: círculo rúnico + buraco negro · Falling Star: corte cósmico na direção da investida
- Prismatic Edge: fitas arco-íris · Lunar Tide: corte cósmico horizontal varrendo
- Crimson Nova: círculo rúnico na carga + sol na detonação · Ethereal Veil: olho de dragão
- Solar Flame: sol · Glimmer Frost: círculo rúnico · Arcane Tempest: círculo rúnico vertical à frente
- Luminous Root: círculo rúnico · Eclipse: buraco negro + olho de dragão
- Wandering Comet: fitas arco-íris na explosão · Cosmic Pulse: buraco negro + círculo rúnico
- Radiant Phoenix: fitas âmbar (asas) + sol

Config nova (`[global]`): `cinematicEnabled`, `cinematicOverlays`, `cinematicIntensity`.
A cadeia do PostChain agora tem 8 passes (distorção/lente → extração → 4 blurs → composição → overlay). Os overlays e o buraco negro dependem do PostChain, então precisam do mesmo `runClient` para validar; se o log mostrar erro de GLSL, ele aponta o `.fsh`.
Teste rápido: troque `bloom_composite` por `fragColor = vec4(base, 1.0);` para isolar bloom de lente/overlay.

## Compilar e testar
1. Requisitos: JDK 17. Este zip **não inclui o Gradle Wrapper**: copie `gradlew`, `gradlew.bat` e a pasta `gradle/` do MDK oficial do Forge 1.20.1 (47.x) para esta pasta, ou rode `gradle wrapper` se tiver Gradle instalado.
2. `./gradlew runClient` (primeira vez baixa as dependências).
3. No jogo: modo criativo → aba **Lumin Blades** → pegue as espadas. Botão direito = habilidade; clique esquerdo = golpe.
4. Compilar o jar: `./gradlew build` → `build/libs/luminblades-1.0.0.jar`.

## Validar Bloom e PostChain
- **Carregou?** No log, procure `PostChain luminblades:shaders/post/lumin_bloom.json carregado`. Se aparecer `Falha ao carregar PostChain`, o stack trace aponta o JSON/shader com erro (nome de uniform, sampler ou erro de GLSL).
- **Bloom:** segure uma espada em local escuro → halo ao redor da lâmina. Ao guardar a espada, o brilho some suavemente (e o PostChain para de rodar).
- **Ligar/desligar:** edite `bloomEnabled` no toml (recarregue o mundo) e compare. Ajuste `bloomThreshold` (menor = mais halo) e `bloomIntensity`.
- **Shaders hot-reload:** edite os `.fsh` em `src/main/resources/...` e use **F3+T** (a cadeia é fechada e recriada no reload).
- **Distorção:** use a Crimson Nova, Cosmic Pulse ou Ethereal Veil; deve aparecer uma onda em anel com leve aberração cromática.
- **Debug de passes:** troque temporariamente `bloom_composite.fsh` para `fragColor = vec4(bloom, 1.0);` para ver só o halo.

## Pontos para ajustar na primeira compilação
- Nomes exatos de API (Forge 47.x) podem variar um pouco entre versões menores.
- Posições da aura/rastro são aproximações da posição da mão (não seguem o osso do braço do modelo).
- Sons usam eventos vanilla; troque por `SoundEvent` próprios se quiser.
