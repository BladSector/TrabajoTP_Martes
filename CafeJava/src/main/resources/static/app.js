"use strict";

const $ = id => document.getElementById(id);
const money = value => new Intl.NumberFormat("es-AR", {style:"currency", currency:"ARS"}).format(value || 0);
const round = value => Math.round(value * 100) / 100;
const escapeHtml = value => String(value ?? "").replace(/[&<>"']/g, character => ({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#39;"}[character]));
const icon = name => '<i data-lucide="' + name + '"></i>';
const normalize = value => String(value || "").normalize("NFD").replace(/[\u0300-\u036f]/g,"").toLowerCase();
const labels = {combos:"Combos",cafeteria:"Cafeter\u00eda",bebidas:"Bebidas",salados:"Salados",dulces:"Dulces","choco-late":"Choco-late"};
const categoryIcons = {combos:"utensils",cafeteria:"coffee",bebidas:"cup-soda",salados:"sandwich",dulces:"cake-slice","choco-late":"candy"};
const stateLabels = {BORRADOR:"Borrador",PENDIENTE:"Pendiente",EN_PREPARACION:"En preparaci\u00f3n",LISTO:"Listo",ENTREGADO:"Entregado"};
const stateClasses = {BORRADOR:"draft",PENDIENTE:"pending",EN_PREPARACION:"preparing",LISTO:"ready",ENTREGADO:"ready"};
const roleFromUrl = location.pathname === "/cocina" || new URLSearchParams(location.search).get("perfil") === "cocina" ? "cocina" : "mozo";
const initialView = {"/entregas":"entregas","/gestion-menu":"menu","/historial-tickets":"tickets","/historial-pedidos":"historial","/resumen-diario":"cierres"}[location.pathname] || "mesas";
const state = {
  perfil:roleFromUrl,view:roleFromUrl==="cocina"?"cocina":initialView,mesas:[],productos:[],categorias:[],
  cocina:[],tickets:[],estado:null,mesaId:null,cuenta:null,cart:[],cartId:1,categoriaPedido:"",
  detalleProducto:null,detalleModo:"notas",cartEditId:null,cafeProducto:null,cafeCliente:"",seleccion:new Map(),pagoPedidos:[],ticketId:null,cierreImpresion:null,
  cambios:new Map(),nuevoId:-1,resumen:null,busy:0,polling:false,mesaSignature:"",cocinaSignature:"",
  entregas:[],entregasFiltro:"mios",entregasSignature:"",entregasKnown:new Set(),entregasMozo:"",
  perfiles:[],perfilMozoId:null,cartPerfilId:null
};
let toastTimer, confirmResolve;

function icons() { if (window.lucide) window.lucide.createIcons(); }
function hideToast() {
  clearTimeout(toastTimer);
  if($("toast").matches(":popover-open"))$("toast").hidePopover();
  $("toast").hidden=true;
}
function toast(message,error=false,loading=false) {
  const aviso=$("toast"),duration=error?5000:2600;
  hideToast();
  const dialogs=[...document.querySelectorAll("dialog[open]")];
  (dialogs.at(-1)||document.body).append(aviso);
  $("toastMessage").textContent=message;$("toastIcon").innerHTML=icon(loading?"loader-circle":error?"circle-alert":"circle-check");
  aviso.classList.toggle("error",error);aviso.classList.toggle("loading",loading);
  aviso.style.setProperty("--toast-duration",duration+"ms");
  aviso.hidden=false;aviso.showPopover();icons();
  if(!loading)toastTimer=setTimeout(hideToast,duration);
}
async function api(path,options={}) {
  const response=await fetch("/api"+path,{...options,headers:{"Content-Type":"application/json",...options.headers}});
  const text=await response.text();
  let data=null;
  if(text) { try {data=JSON.parse(text);} catch {data=text;} }
  if(!response.ok) throw new Error(data?.error||"No se pudo completar la operacion");
  return data;
}
const post=(path,body)=>api(path,{method:"POST",body:body===undefined?undefined:JSON.stringify(body)});
async function action(button,operation) {
  if(button?.disabled||state.busy) return;
  state.busy++;if(button) {button.disabled=true;button.classList.add("is-busy");button.setAttribute("aria-busy","true");}updateButtons();
  try {await operation();} catch(error) {toast(error.message,true);}
  finally {state.busy--;if(button?.isConnected) {button.disabled=false;button.classList.remove("is-busy");button.removeAttribute("aria-busy");}updateButtons();}
}
function confirmAction(title,message) {
  if(confirmResolve) return Promise.resolve(false);
  $("confirmTitle").textContent=title;$("confirmMessage").textContent=message;
  $("confirmDialog").showModal();
  return new Promise(resolve=>confirmResolve=resolve);
}
function resolveConfirm(answer) {
  const resolve=confirmResolve;confirmResolve=null;$("confirmDialog").close();if(resolve)resolve(answer);
}
async function closeDialog(dialog) {
  if(dialog.id==="pedidosDialog"&&state.cart.length) {
    if(!await confirmAction("Pedidos pendientes","Descartar los productos que todavia no se enviaron a cocina?"))return;
  }
  dialog.close();
  if(dialog.id==="pedidosDialog") {state.cart=[];renderCart();}
}
function categoryLabel(name) {return labels[name]||name;}
function orderedCategories() {
  const order=Object.keys(labels);
  return [...state.categorias].sort((a,b)=>(order.indexOf(a.nombre)<0?99:order.indexOf(a.nombre))-(order.indexOf(b.nombre)<0?99:order.indexOf(b.nombre))||a.nombre.localeCompare(b.nombre));
}
function categoryOptions(selected="") {
  return orderedCategories().map(c=>'<option value="'+c.id+'" '+(String(c.id)===String(selected)?"selected":"")+'>'+escapeHtml(categoryLabel(c.nombre))+'</option>').join("");
}
function updateButtons() {
  $("mozo").disabled=!!state.busy;
  document.querySelectorAll("[data-usar-perfil],[data-borrar-perfil]").forEach(button=>button.disabled=!!state.busy);
  $("enviarCocina").disabled=!!state.estado?.cerrado||!state.cart.length||!!state.busy;
  $("aplicarCambios").disabled=!state.cambios.size||!!state.busy;
  $("descartarCambios").disabled=!state.cambios.size||!!state.busy;
  $("confirmarPago").disabled=!!state.estado?.cerrado||!selectedDetails().length||!!state.busy||(!$("totalEfectivo").disabled&&!$("totalEfectivo").validity.valid);
  $("cerrarDia").disabled=!!state.resumen?.id||!!state.busy;
  document.querySelectorAll("[data-cocina-pedido]").forEach(button=>button.disabled=!!state.busy);
  document.querySelectorAll("[data-entregar]").forEach(button=>{
    const pedido=state.entregas.find(c=>c.pedido.id===Number(button.dataset.entregar))?.pedido;
    button.disabled=!!state.busy||!esPedidoPropio(pedido);
  });
}
function categoryBadge(pedido) {
  return '<span class="badge '+stateClasses[pedido.estadoCocina]+'">'+stateLabels[pedido.estadoCocina]+'</span>';
}
function remaining(pedido) {return pedido.cancelado?0:pedido.cantidad-pedido.cantidadPagada;}
function renderMesaItems(pedidos) {
  return pedidos.map(p=>'<div class="consumo-line"><span class="badge">'+remaining(p)+'x</span><div><strong>'+escapeHtml(p.nombreProducto)+'</strong>'+(p.notas?'<small>'+escapeHtml(p.notas)+'</small>':"")+'<small>'+escapeHtml(p.mozo)+'</small></div>'+categoryBadge(p)+'</div>').join("");
}
function renderMesas() {
  const abiertos=new Set([...document.querySelectorAll("#mapaMesas details[open]")].map(d=>d.dataset.details));
  $("ocupadas").textContent=state.mesas.filter(v=>v.mesa.estado==="OCUPADA").length+" mesas ocupadas / "+state.mesas.length+" mesas";
  $("mapaMesas").innerHTML=state.mesas.map(v=>{
    const activos=v.pedidos.filter(p=>remaining(p)>0), unidades=activos.reduce((sum,p)=>sum+remaining(p),0);
    const cobrables=activos.some(p=>p.estadoCocina!=="BORRADOR"), ocupada=v.mesa.estado==="OCUPADA";
    let contenido='<div class="mesa-empty">'+icon("coffee")+'Sin consumos</div>';
    if(activos.length) {
      contenido='<div class="mesa-summary"><span>'+unidades+' productos pendientes</span><strong>'+money(v.pendiente)+'</strong></div>';
      if(v.mesa.esTakeAway) {
        const clientes=Map.groupBy?Map.groupBy(activos,p=>p.nombreCliente):groupBy(activos,p=>p.nombreCliente);
        contenido+=[...clientes].map(([cliente,items])=>'<div class="client-group"><div class="client-group-title"><span>'+icon("user-round")+' '+escapeHtml(cliente)+'</span><button data-pagar="'+v.mesa.id+'" data-cliente="'+escapeHtml(cliente)+'" '+(state.estado?.cerrado?"disabled":"")+'>Cobrar cliente</button></div>'+renderMesaItems(items)+'</div>').join("");
      } else {
        contenido+='<details data-details="'+v.mesa.id+'" '+(abiertos.has(String(v.mesa.id))?"open":"")+'><summary>Pedidos &middot; '+unidades+' productos</summary>'+renderMesaItems(activos)+'</details>';
      }
    }
    return '<article class="mesa-card '+(ocupada?"occupied":"")+'" data-mesa="'+v.mesa.id+'"><div class="mesa-card-header"><h2>'+icon(v.mesa.esTakeAway?"shopping-bag":"utensils")+escapeHtml(v.mesa.nombre)+'</h2><span class="badge '+(ocupada?"occupied":"free")+'">'+(ocupada?"Ocupada":"Libre")+'</span></div><div class="mesa-card-body">'+contenido+'</div><div class="mesa-card-footer"><button class="outline-primary" data-pedidos="'+v.mesa.id+'" '+(state.estado?.cerrado?"disabled":"")+'>'+icon("plus")+'Agregar pedidos</button>'+(cobrables?'<button class="success" data-pagar="'+v.mesa.id+'" '+(state.estado?.cerrado?"disabled":"")+'>'+icon("credit-card")+'Pagar cuenta</button>':"")+(v.cuentaId?'<button class="icon-button" data-consumos="'+v.mesa.id+'" title="Ver consumos" aria-label="Ver consumos">'+icon("list")+'</button>':"")+'</div></article>';
  }).join("");
  icons();
}
function groupBy(items,key) {
  const groups=new Map();
  for(const item of items) {const value=key(item);if(!groups.has(value))groups.set(value,[]);groups.get(value).push(item);}
  return groups;
}
async function loadMesas(force=false) {
  state.mesas=await api("/mesas");
  const signature=JSON.stringify([state.estado?.cerrado,state.mesas]);
  if(force||signature!==state.mesaSignature) {state.mesaSignature=signature;renderMesas();}
}
async function loadMenu() {
  [state.productos,state.categorias]=await Promise.all([api("/menu"),api("/categorias")]);
  const categoria=$("menuCategoria").value;
  $("menuCategoria").innerHTML='<option value="">Todas las categorias</option>'+categoryOptions(categoria);
  renderMenu();
}
async function openOrders(mesaId) {
  if(!state.estado)await loadStatus();
  if(state.estado.cerrado)throw new Error("La jornada esta cerrada");
  if(!state.perfilMozoId) {await openPerfiles();throw new Error("Seleccione o guarde un perfil de mozo");}
  state.cartPerfilId=state.perfilMozoId;
  await loadMenu();state.mesaId=mesaId;state.cart=[];state.categoriaPedido="";
  const vista=state.mesas.find(v=>v.mesa.id===mesaId);
  $("pedidosMesaNombre").textContent=vista.mesa.nombre;
  $("buscarPlato").value="";$("pedidoCliente").value="";
  $("pedidoCliente").required=vista.mesa.esTakeAway;
  if(vista.mesa.esTakeAway) {$("clienteForm").reset();$("clienteDialog").showModal();}
  else showOrders();
}
function showOrders() {
  renderCatalog();renderCart();$("pedidosDialog").showModal();
  $("pedidosDialog").scrollTop=0;$("pedidosPendientesContainer").scrollTop=0;$("listaMenu").scrollTop=0;icons();
}
function renderCatalog() {
  const query=normalize($("buscarPlato").value);
  $("filtrosCategorias").innerHTML='<button class="'+(!state.categoriaPedido?"active":"")+'" data-categoria-pedido="">Todos</button>'+orderedCategories().map(c=>'<button data-categoria-pedido="'+c.id+'" class="'+(String(c.id)===state.categoriaPedido?"active":"")+'">'+escapeHtml(categoryLabel(c.nombre))+'</button>').join("");
  const productos=state.productos.filter(p=>p.disponible&&(!state.categoriaPedido||String(p.categoria.id)===state.categoriaPedido)&&normalize(p.nombre+" "+p.descripcion).includes(query));
  const grupos=groupBy(productos,p=>p.categoria.id);
  $("listaMenu").innerHTML=orderedCategories().filter(c=>grupos.has(c.id)).map(c=>'<section class="catalog-category"><h3>'+icon(categoryIcons[c.nombre]||"utensils")+escapeHtml(categoryLabel(c.nombre))+'</h3><div class="product-grid">'+grupos.get(c.id).map(p=>'<button type="button" class="product-tile" data-producto="'+p.id+'"><span class="product-name">'+escapeHtml(p.nombre)+'</span>'+(p.descripcion?'<span class="product-description">'+escapeHtml(p.descripcion)+'</span>':"")+'<strong>'+money(p.precio)+'</strong><span class="product-add">'+icon("plus")+'Agregar</span></button>').join("")+'</div></section>').join("")||'<p class="empty-column">Sin productos disponibles</p>';
  $("listaMenu").scrollTop=0;icons();
}
function isCoffee(producto) {
  return ["pocillo","jarrito","taza","tazon","cafe para llevar chico","cafe para llevar mediano","cafe para llevar grande"].includes(normalize(producto.nombre));
}
function selectProduct(id) {
  if(state.busy)return;
  if($("pedidoCliente").required&&!$("pedidoCliente").value.trim()) {$("pedidoCliente").reportValidity();return;}
  const producto=state.productos.find(p=>p.id===id);
  if(isCoffee(producto)) {
    state.cafeProducto=producto;state.cafeCliente=$("pedidoCliente").value.trim();
    $("tipoCafeProducto").textContent=producto.nombre;$("tipoCafeDialog").showModal();return;
  }
  try {addPendingProduct(producto,"",$("pedidoCliente").value.trim());}
  catch(error) {toast(error.message,true);}
}
function addPendingProduct(producto,notas,cliente) {
  const item=addCartItem(producto,1,notas,cliente);
  notifyAddedProduct(producto,item,notas);
}
function notifyAddedProduct(producto,item,notas) {
  renderCart();
  document.querySelector('[data-cart-quantity="'+item.id+'"]').closest(".cart-item").classList.add("just-added");
  toast("1x "+producto.nombre+(notas?" - "+notas.replace("Tipo: ",""):"")+" agregado a la tanda");
}
function chooseCoffee(type) {
  if(!state.cafeProducto)return;
  try {
    const producto=state.cafeProducto,notas="Tipo: "+type;
    const item=addCartItem(producto,1,notas,state.cafeCliente);
    state.cafeProducto=null;$("tipoCafeDialog").close();
    notifyAddedProduct(producto,item,notas);
  } catch(error) {toast(error.message,true);}
}
function configureProduct(id,item,mode="notas") {
  const producto=state.productos.find(p=>p.id===id);
  state.detalleProducto=producto;state.detalleModo=mode;state.cartEditId=item.id;$("detalleProductoForm").reset();
  $("detalleProductoNombre").textContent=producto.nombre;$("detalleProductoPrecio").textContent=money(producto.precio);
  $("tipoCafeFieldset").hidden=mode!=="cafe";
  $("pedidoNotasLabel").hidden=mode!=="notas";
  $("detalleProductoSubmit").textContent=mode==="cafe"?"Guardar tipo de cafe":"Guardar comentario";
  const notas=item.notas.split("\n"),tipo=isCoffee(producto)&&notas[0]?.startsWith("Tipo: ")?notas.shift().slice(6):null;
  if(tipo)$("detalleProductoForm").elements.tipoCafe.value=tipo;
  $("pedidoNotas").value=notas.join("\n");
  $("detalleProductoDialog").showModal();
  if(mode==="notas")$("pedidoNotas").focus();
}
function renderCart() {
  const unidades=state.cart.reduce((sum,p)=>sum+p.cantidad,0);
  $("cartCount").textContent=unidades;
  $("cartTotal").textContent=money(round(state.cart.reduce((sum,p)=>sum+p.cantidad*p.precio,0)));
  $("pedidosPendientesContainer").innerHTML=state.cart.length?state.cart.map(p=>'<div class="cart-item"><div class="cart-item-title"><strong>'+escapeHtml(p.nombre)+'</strong><div class="cart-item-actions">'+(isCoffee(state.productos.find(producto=>producto.id===p.productoId))?'<button class="icon-button" data-cart-coffee="'+p.id+'" title="Elegir tipo de cafe" aria-label="Elegir tipo de cafe">'+icon("coffee")+'</button>':"")+'<button class="icon-button" data-cart-edit="'+p.id+'" title="Comentario" aria-label="Comentario">'+icon("message-square")+'</button><button class="icon-button danger" data-cart-remove="'+p.id+'" title="Eliminar del pedido" aria-label="Eliminar del pedido">'+icon("trash-2")+'</button></div></div>'+(p.notas?'<small class="notes">'+escapeHtml(p.notas)+'</small>':"")+(p.nombreCliente?'<small>'+escapeHtml(p.nombreCliente)+'</small>':"")+'<small>'+money(p.precio)+' c/u</small><div class="cart-item-footer"><div class="quantity-stepper"><button data-cart-step="-1" data-cart-id="'+p.id+'" title="Restar cantidad" aria-label="Restar cantidad">'+icon("minus")+'</button><input class="cart-quantity" data-cart-quantity="'+p.id+'" type="number" value="'+p.cantidad+'" min="1" max="1000" aria-label="Cantidad de '+escapeHtml(p.nombre)+'"><button data-cart-step="1" data-cart-id="'+p.id+'" title="Sumar cantidad" aria-label="Sumar cantidad">'+icon("plus")+'</button></div><strong>'+money(p.precio*p.cantidad)+'</strong></div></div>').join(""):'<div class="empty-column">No hay pedidos pendientes</div>';
  updateButtons();icons();
}
function addConfiguredProduct() {
  const producto=state.detalleProducto,item=state.cart.find(p=>p.id===state.cartEditId);
  if(!item)throw new Error("El producto ya no esta en la tanda");
  const tipoAnterior=isCoffee(producto)&&item.notas.startsWith("Tipo: ")?item.notas.split("\n")[0]:"";
  const tipo=state.detalleModo==="cafe"?"Tipo: "+$("detalleProductoForm").elements.tipoCafe.value:tipoAnterior;
  const notas=[tipo,$("pedidoNotas").value.trim()].filter(Boolean).join("\n");
  addCartItem(producto,item.cantidad,notas,item.nombreCliente,item);
  $("detalleProductoDialog").close();renderCart();toast(state.detalleModo==="cafe"?"Tipo de cafe actualizado":"Comentario guardado");
}
function addCartItem(producto,cantidad,notas,cliente,item=null) {
  const existente=state.cart.find(p=>p.id!==item?.id&&p.productoId===producto.id&&p.notas===notas&&p.nombreCliente===cliente);
  if(existente) {
    if(existente.cantidad+cantidad>1000)throw new Error("Cantidad maxima de 1000");
    existente.cantidad+=cantidad;
    if(item)state.cart=state.cart.filter(p=>p.id!==item.id);
  } else if(item) {
    Object.assign(item,{cantidad,notas});
  } else {
    if(state.cart.length>=100)throw new Error("La comanda admite hasta 100 productos");
    state.cart.push({id:state.cartId++,productoId:producto.id,nombre:producto.nombre,precio:producto.precio,cantidad,notas,nombreCliente:cliente});
  }
  return existente||item||state.cart[state.cart.length-1];
}
async function sendComanda() {
  const pedidos=state.cart.map(({productoId,cantidad,notas,nombreCliente})=>({productoId,cantidad,notas,nombreCliente,perfilMozoId:state.cartPerfilId}));
  const unidades=pedidos.reduce((sum,p)=>sum+p.cantidad,0);
  toast("Registrando tanda...",false,true);
  await post("/mesas/"+state.mesaId+"/comandas",{pedidos});
  state.cart=[];$("pedidosDialog").close();renderCart();
  toast("Tanda confirmada: "+unidades+" productos enviados a cocina");await loadMesas(true);
}
async function openConsumos(mesaId) {
  state.mesaId=mesaId;state.cuenta=(await api("/mesas/"+mesaId+"/cuenta")).cuenta;
  if(!state.cuenta)throw new Error("La mesa no tiene una cuenta abierta");
  $("consumosTitle").textContent="Consumos \u00b7 "+state.cuenta.mesa.nombre;
  $("consumosBody").innerHTML=state.cuenta.pedidos.map(p=>'<tr><td><strong>'+escapeHtml(p.nombreProducto)+'</strong><small>'+escapeHtml(p.notas)+'</small></td><td>'+p.cantidad+'</td><td>'+(p.cancelado?'<span class="badge">Cancelado</span>':categoryBadge(p))+'</td><td>'+p.cantidadPagada+'</td><td>'+money(p.precioUnitario*p.cantidad)+'</td><td>'+(!p.cancelado&&!p.cantidadPagada?'<button class="icon-button danger" data-cancelar="'+p.id+'" title="Cancelar pedido" aria-label="Cancelar pedido">'+icon("trash-2")+'</button>':"")+'</td></tr>').join("")||'<tr><td colspan="6" class="muted">Sin consumos</td></tr>';
  $("enviarBorradores").hidden=!state.cuenta.pedidos.some(p=>!p.cancelado&&p.estadoCocina==="BORRADOR");
  $("liberarMesa").hidden=state.cuenta.pedidos.some(p=>remaining(p)>0);
  if(!$("consumosDialog").open)$("consumosDialog").showModal();icons();
}
function selectedDetails() {
  return state.pagoPedidos.map(p=>({pedidoId:p.id,cantidad:Number(state.seleccion.get(p.id)||0),precio:p.precioUnitario})).filter(d=>d.cantidad>0);
}
function selectedSubtotal() {return round(selectedDetails().reduce((sum,d)=>sum+d.cantidad*d.precio,0));}
async function openPayment(mesaId,cliente=null) {
  state.mesaId=mesaId;state.cuenta=(await api("/mesas/"+mesaId+"/cuenta")).cuenta;
  if(!state.cuenta)throw new Error("La mesa no tiene una cuenta abierta");
  state.pagoPedidos=state.cuenta.pedidos.filter(p=>remaining(p)>0&&p.estadoCocina!=="BORRADOR"&&(cliente===null||p.nombreCliente===cliente));
  if(!state.pagoPedidos.length)throw new Error("No hay consumos para pagar");
  state.seleccion=new Map(state.pagoPedidos.map(p=>[p.id,remaining(p)]));
  $("pagoMesaNombre").textContent=state.cuenta.mesa.nombre+(cliente?" \u00b7 "+cliente:"");
  $("pagoForm").reset();renderPayment();$("pagoDialog").showModal();
}
function renderPayment() {
  const grupos=groupBy(state.pagoPedidos,p=>p.nombreCliente||"Mesa general");
  $("pagoItems").innerHTML=[...grupos].map(([cliente,items])=>'<div class="payment-group"><div class="payment-group-heading">'+escapeHtml(cliente)+'</div>'+items.map(p=>'<div class="payment-item"><input type="checkbox" class="pay-check" data-pay-check="'+p.id+'" checked aria-label="Seleccionar '+escapeHtml(p.nombreProducto)+'"><div><strong>'+escapeHtml(p.nombreProducto)+'</strong><small>'+escapeHtml(p.notas)+'</small><small>'+money(p.precioUnitario)+' c/u \u00b7 '+remaining(p)+' pendientes</small></div><input class="pay-quantity" data-pago="'+p.id+'" type="number" min="0" max="'+remaining(p)+'" value="'+remaining(p)+'" aria-label="Cantidad a cobrar de '+escapeHtml(p.nombreProducto)+'"></div>').join("")+'</div>').join("");
  updatePayment();
}
function updatePayment() {
  const subtotal=selectedSubtotal(),efectivo=$("pagoForm").elements.metodo.value==="EFECTIVO";
  const input=$("totalEfectivo"),base=round(subtotal-round(subtotal*.10));
  const margen=Math.floor(Math.round(base*100)/20)/100;
  const minimo=round(Math.max(.01,base-margen)),maximo=round(Math.min(subtotal,base+margen));
  input.min=minimo.toFixed(2);input.max=maximo.toFixed(2);input.disabled=!efectivo||base<=0;
  input.setCustomValidity("");
  const manual=efectivo&&input.value!==""&&!input.disabled;
  const fuera=manual&&(!Number.isFinite(Number(input.value))||Number(input.value)<minimo||Number(input.value)>maximo);
  const mensaje="El total debe estar entre "+money(minimo)+" y "+money(maximo)+" (5% del total con descuento)";
  if(fuera)input.setCustomValidity(mensaje);
  const invalido=manual&&!input.validity.valid;
  input.setAttribute("aria-invalid",String(invalido));
  $("redondeoRango").textContent=base>0?"Permitido: "+money(minimo)+" a "+money(maximo):"";
  $("redondeoError").textContent=fuera?mensaje:"Importe invalido: use hasta dos decimales";
  $("redondeoError").hidden=!invalido;
  $("totalEfectivoLabel").hidden=!efectivo;
  const total=efectivo?(manual&&!invalido?Number(input.value):base):subtotal;
  $("pagoSubtotal").textContent=money(subtotal);$("pagoDescuento").textContent=money(round(subtotal-total));$("pagoTotal").textContent=invalido?"Fuera de rango":money(total);
  $("seleccionarTodos").checked=state.pagoPedidos.every(p=>state.seleccion.get(p.id)===remaining(p));
  updateButtons();
}
async function payCuenta() {
  const metodo=$("pagoForm").elements.metodo.value;
  toast("Registrando pago...",false,true);
  const respuesta=await post("/cuentas/"+state.cuenta.id+"/pagos",{
    metodo,detalles:selectedDetails().map(({pedidoId,cantidad})=>({pedidoId,cantidad})),
    totalEfectivo:metodo==="EFECTIVO"&&$("totalEfectivo").value?Number($("totalEfectivo").value):null
  });
  $("pagoDialog").close();state.seleccion.clear();await loadMesas(true);await openTicket(respuesta.ticket.id);
  toast((respuesta.cuentaCerrada?"Pago registrado. Mesa libre. ":"Pago parcial registrado. ")+money(respuesta.pago.total));
}
function menuProducts() {
  const productos=new Map(state.productos.map(p=>[p.id,p]));
  for(const [id,cambio] of state.cambios)productos.set(id,{...cambio.producto,pendiente:cambio.tipo});
  return [...productos.values()].sort((a,b)=>a.categoria.id-b.categoria.id||a.nombre.localeCompare(b.nombre));
}
function renderMenu() {
  const query=normalize($("menuBuscar").value),categoria=$("menuCategoria").value;
  const productos=menuProducts().filter(p=>(!categoria||String(p.categoria.id)===categoria)&&normalize(p.nombre+" "+p.descripcion).includes(query));
  const cambios={CREAR:"Nuevo",ACTUALIZAR:"Modificado",ELIMINAR:"Se eliminara"};
  $("menuBody").innerHTML=productos.length?productos.map(p=>'<tr class="'+(p.pendiente==="ELIMINAR"?"deleted-row":"")+'"><td><strong>'+escapeHtml(p.nombre)+'</strong><small>'+escapeHtml(p.descripcion)+'</small></td><td>'+escapeHtml(categoryLabel(p.categoria.nombre))+'</td><td>'+money(p.precio)+'</td><td><input type="checkbox" data-disponible="'+p.id+'" '+(p.disponible?"checked":"")+' '+(p.pendiente==="ELIMINAR"?"disabled":"")+' aria-label="Disponibilidad de '+escapeHtml(p.nombre)+'"></td><td><span class="badge '+(p.pendiente?"pending":"")+'">'+(cambios[p.pendiente]||"Guardado")+'</span></td><td><div class="table-actions">'+(p.pendiente==="ELIMINAR"?'<button class="icon-button" data-restaurar="'+p.id+'" title="Deshacer eliminacion" aria-label="Deshacer eliminacion">'+icon("undo-2")+'</button>':'<button class="icon-button" data-editar="'+p.id+'" title="Editar plato" aria-label="Editar plato">'+icon("pencil")+'</button><button class="icon-button danger" data-eliminar="'+p.id+'" title="Eliminar plato" aria-label="Eliminar plato">'+icon("trash-2")+'</button>')+'</div></td></tr>').join(""):'<tr><td colspan="6" class="muted">Sin productos</td></tr>';
  $("cantidadCambios").textContent=state.cambios.size?state.cambios.size+" cambios pendientes":"Sin cambios pendientes";
  updateButtons();icons();
}
function openProducto(producto=null) {
  $("productoForm").reset();$("productoId").value=producto?.id??"";
  $("productoDialogTitle").textContent=producto?"Editar plato":"Nuevo plato";
  $("productoCategoria").innerHTML=categoryOptions(producto?.categoria.id);
  $("productoNombre").value=producto?.nombre||"";$("productoDescripcion").value=producto?.descripcion||"";
  $("productoPrecio").value=producto?.precio??"";$("productoDisponible").checked=producto?.disponible??true;
  $("productoDialog").showModal();
}
function stageProduct() {
  const id=$("productoId").value===""?state.nuevoId--:Number($("productoId").value);
  const producto={id,nombre:$("productoNombre").value.trim(),descripcion:$("productoDescripcion").value,precio:Number($("productoPrecio").value),disponible:$("productoDisponible").checked,categoria:state.categorias.find(c=>c.id===Number($("productoCategoria").value))};
  state.cambios.set(id,{tipo:id<0?"CREAR":"ACTUALIZAR",producto});
  $("productoDialog").close();renderMenu();toast("Producto agregado a los cambios pendientes");
}
function productBody(producto) {
  return {nombre:producto.nombre,descripcion:producto.descripcion,precio:producto.precio,disponible:producto.disponible,categoriaId:producto.categoria.id};
}
async function applyMenu() {
  const cambios=[...state.cambios].map(([id,c])=>({tipo:c.tipo,id:id<0?null:id,producto:c.tipo==="ELIMINAR"?null:productBody(c.producto)}));
  toast("Guardando menu...",false,true);
  await post("/menu/cambios",{cambios});state.cambios.clear();await loadMenu();toast("Cambios del menu guardados");
}
function renderTickets() {
  const fecha=$("ticketFecha").value,filtered=state.tickets.filter(t=>!fecha||t.fechaEmision.startsWith(fecha));
  const total=filtered.reduce((sum,t)=>sum+t.total,0),efectivo=filtered.filter(t=>t.pago.metodo==="EFECTIVO").reduce((sum,t)=>sum+t.total,0);
  $("resumenTickets").innerHTML=summaryHtml([["Tickets",filtered.length],["Efectivo",money(efectivo)],["Tarjeta",money(total-efectivo)],["Descuentos",money(filtered.reduce((sum,t)=>sum+t.descuento,0))],["Total",money(total)]]);
  $("ticketsBody").innerHTML=filtered.length?filtered.map(t=>'<tr><td><button class="secondary" data-ticket="'+t.id+'">#'+t.id+'</button></td><td>'+new Date(t.fechaEmision).toLocaleString("es-AR")+'</td><td>'+t.pago.cuentaId+'</td><td>'+(t.pago.metodo==="EFECTIVO"?"Efectivo":"Tarjeta")+'</td><td>'+money(t.total)+'</td><td><span class="badge draft">Prueba</span></td><td><button class="icon-button" data-ticket="'+t.id+'" title="Ver comprobante" aria-label="Ver comprobante">'+icon("receipt")+'</button></td></tr>').join(""):'<tr><td colspan="7" class="muted">Sin tickets</td></tr>';icons();
}
async function loadTickets() {state.tickets=await api("/tickets");renderTickets();}
async function openTicket(id) {
  state.ticketId=id;state.cierreImpresion=null;
  $("ticketContenido").textContent=await api("/tickets/"+id+"/texto");$("ticketDialog").showModal();
}
async function openCierre(fecha) {
  state.ticketId=null;state.cierreImpresion=fecha;
  $("ticketContenido").textContent=await api("/cierres/texto?fecha="+fecha);$("ticketDialog").showModal();
}
function summaryHtml(values) {return values.map(([label,value])=>'<div><span>'+label+'</span><strong>'+value+'</strong></div>').join("");}
async function loadCierres() {
  const fecha=$("cierreFecha").value||state.estado.fecha;
  const [resumen,historial,tickets]=await Promise.all([api("/cierres/resumen?fecha="+fecha),api("/cierres"),api("/tickets")]);
  state.resumen=resumen;
  $("resumenCierre").innerHTML=summaryHtml([["Tickets",resumen.cantidadTickets],["Efectivo",money(resumen.totalEfectivo)],["Tarjeta",money(resumen.totalTarjeta)],["Descuentos",money(resumen.totalDescuentos)],["Total",money(resumen.totalGeneral)]]);
  $("cerrarDia").innerHTML=icon(resumen.id?"lock-keyhole":"calendar-check")+(resumen.id?"Jornada cerrada":"Cerrar jornada");
  $("ventasBody").innerHTML=tickets.filter(t=>t.fechaEmision.startsWith(fecha)).flatMap(t=>t.items.map(i=>'<tr><td>'+new Date(t.fechaEmision).toLocaleTimeString("es-AR",{hour:"2-digit",minute:"2-digit"})+'</td><td>'+escapeHtml(i.descripcion)+'</td><td>'+i.cantidad+'</td><td>'+money(i.precioUnitario)+'</td><td>'+money(i.precioUnitario*i.cantidad)+'</td><td>'+(t.pago.metodo==="EFECTIVO"?"Efectivo":"Tarjeta")+'</td></tr>')).join("")||'<tr><td colspan="6" class="muted">Sin ventas</td></tr>';
  $("cierresBody").innerHTML=historial.length?historial.map(c=>'<tr><td>'+c.fecha+'</td><td>'+c.cantidadTickets+'</td><td>'+money(c.totalEfectivo)+'</td><td>'+money(c.totalTarjeta)+'</td><td>'+money(c.totalDescuentos)+'</td><td><strong>'+money(c.totalGeneral)+'</strong></td><td><button class="icon-button" data-cierre="'+c.fecha+'" title="Ver cierre" aria-label="Ver cierre">'+icon("printer")+'</button></td></tr>').join(""):'<tr><td colspan="7" class="muted">Sin cierres registrados</td></tr>';
  updateButtons();icons();
}
function renderCocina() {
  const columnas=[["PENDIENTE","pendientes","EN_PREPARACION","Preparar"],["EN_PREPARACION","preparacion","LISTO","Marcar listo"],["LISTO","listos",null,null]];
  const query=normalize($("cocinaBuscar").value);
  const visibles=state.cocina.filter(c=>normalize([c.mesa.nombre,c.pedido.nombreProducto,c.pedido.mozo,c.pedido.nombreCliente,c.pedido.notas].join(" ")).includes(query));
  for(const [estado,id,siguiente,accion] of columnas) {
    const cola=visibles.filter(c=>c.pedido.estadoCocina===estado).sort((a,b)=>a.pedido.fechaHora.localeCompare(b.pedido.fechaHora)||a.pedido.id-b.pedido.id);
    $("count-"+id).textContent=cola.reduce((sum,c)=>sum+c.pedido.cantidad,0);
    const grupos=groupBy(cola,c=>JSON.stringify([c.pedido.cuentaId,c.pedido.nombreCliente,c.pedido.mozo]));
    $("cocina-"+id).innerHTML=[...grupos.values()].map(items=>{
      const primero=items[0],p=primero.pedido;
      const minutos=Math.max(0,Math.floor((Date.now()-new Date(p.fechaHora).getTime())/60000));
      return '<article class="comanda '+(minutos>=15&&estado!=="LISTO"?"delayed":"")+'"><div class="comanda-heading"><div><h3>'+escapeHtml(primero.mesa.nombre)+'</h3><small>Mozo: '+escapeHtml(p.mozo)+(p.nombreCliente?' \u00b7 '+escapeHtml(p.nombreCliente):"")+' \u00b7 Cuenta '+p.cuentaId+'</small></div><span class="badge '+(minutos>=15&&estado!=="LISTO"?"pending":"")+'" title="Tiempo desde que se tomo el pedido">'+(minutos>=15&&estado!=="LISTO"?"Demora: ":"")+minutos+' min</span></div>'+items.map(({pedido})=>'<div class="comanda-item"><div class="comanda-item-title"><span class="badge">'+pedido.cantidad+'x</span><strong>'+escapeHtml(pedido.nombreProducto)+'</strong>'+(pedido.cantidadPagada===pedido.cantidad?'<span class="badge ready">Pagado</span>':"")+'</div>'+(pedido.notas?'<p class="notes">'+escapeHtml(pedido.notas)+'</p>':"")+(siguiente?'<button class="outline-primary" data-cocina-pedido="'+pedido.id+'" data-next="'+siguiente+'">'+icon("chef-hat")+accion+'</button>':'<p class="ready-handoff">'+icon("user-round")+'Esperando retiro de '+escapeHtml(pedido.mozo)+'</p>')+'</div>').join("")+'</article>';
    }).join("")||'<div class="empty-column">Sin pedidos</div>';
  }
  $("comandasTotal").textContent=visibles.reduce((sum,c)=>sum+c.pedido.cantidad,0)+" productos / "+new Set(visibles.map(c=>c.pedido.cuentaId)).size+" cuentas";
  $("actualizacionCocina").textContent="Actualizado "+new Date().toLocaleTimeString("es-AR",{hour:"2-digit",minute:"2-digit"});
  updateButtons();icons();
}
async function loadCocina(force=false) {
  state.cocina=await api("/cocina");
  const signature=JSON.stringify([Math.floor(Date.now()/60000),state.cocina]);
  if(force||signature!==state.cocinaSignature) {state.cocinaSignature=signature;renderCocina();}
}
function esPedidoPropio(pedido) {return !!state.perfilMozoId&&pedido?.perfilMozoId===state.perfilMozoId;}
function renderPerfiles() {
  $("mozo").innerHTML='<option value="">Elegir perfil</option>'+state.perfiles.map(p=>'<option value="'+p.id+'">'+escapeHtml(p.nombre)+'</option>').join("");
  $("mozo").value=state.perfilMozoId||"";
  $("listaPerfiles").innerHTML=state.perfiles.map(p=>'<div class="saved-profile"><button data-usar-perfil="'+p.id+'" class="'+(p.id===state.perfilMozoId?"active":"")+'">'+icon(p.id===state.perfilMozoId?"circle-check":"user-round")+escapeHtml(p.nombre)+'</button><button class="icon-button danger" data-borrar-perfil="'+p.id+'" title="Eliminar perfil de '+escapeHtml(p.nombre)+'" aria-label="Eliminar perfil de '+escapeHtml(p.nombre)+'">'+icon("trash-2")+'</button></div>').join("")||'<p class="muted">No hay perfiles guardados</p>';
  icons();
}
async function loadPerfiles() {
  const perfiles=await api("/perfiles");
  const elegido=state.perfilMozoId||Number(localStorage.getItem("cafe-perfil-mozo"));
  const id=perfiles.some(p=>p.id===elegido)?elegido:null;
  const changed=JSON.stringify(perfiles)!==JSON.stringify(state.perfiles)||id!==state.perfilMozoId;
  state.perfiles=perfiles;state.perfilMozoId=id;
  if(!id)localStorage.removeItem("cafe-perfil-mozo");
  if(changed) {renderPerfiles();renderEntregas();}
}
function seleccionarPerfil(id) {
  const perfil=state.perfiles.find(p=>p.id===Number(id));
  state.perfilMozoId=perfil?.id||null;
  if(perfil)localStorage.setItem("cafe-perfil-mozo",perfil.id);else localStorage.removeItem("cafe-perfil-mozo");
  renderPerfiles();renderEntregas();
  if(perfil)toast("Perfil activo: "+perfil.nombre);
}
async function openPerfiles() {
  await loadPerfiles();$("perfilForm").reset();$("perfilesDialog").showModal();
}
async function loadHistorial() {
  if(!$("historialFecha").value)$("historialFecha").value=state.estado.fecha;
  const id=$("historialMozo").value;
  const [perfiles,registro]=await Promise.all([api("/perfiles?incluirEliminados=true"),api("/pedidos/historial?fecha="+$("historialFecha").value+(id?"&perfilId="+id:""))]);
  $("historialMozo").innerHTML='<option value="">Todos</option>'+perfiles.map(p=>'<option value="'+p.id+'">'+escapeHtml(p.nombre)+(p.eliminado?" (eliminado)":"")+' #'+p.id+'</option>').join("");
  $("historialMozo").value=id;
  $("historialTotal").textContent=registro.length+" pedidos registrados";
  $("historialBody").innerHTML=registro.map(({pedido:p,mesa})=>'<tr><td><strong>#'+p.id+'</strong><small>'+new Date(p.fechaHora).toLocaleTimeString("es-AR")+'</small></td><td>'+escapeHtml(mesa.nombre)+'<small>Cuenta #'+p.cuentaId+(p.nombreCliente?' / '+escapeHtml(p.nombreCliente):"")+'</small></td><td>'+escapeHtml(p.mozo)+'<small>Perfil #'+p.perfilMozoId+'</small></td><td>'+escapeHtml(p.nombreProducto)+'<small>'+escapeHtml(p.notas)+'</small></td><td>'+p.cantidad+'</td><td>'+(p.cancelado?'<span class="badge">Cancelado</span>':categoryBadge(p))+'</td><td>'+p.cantidadPagada+'</td></tr>').join("")||'<tr><td colspan="7" class="muted">Sin pedidos en esta fecha</td></tr>';
}
function ownReadyOrders() {
  return state.entregas.filter(c=>esPedidoPropio(c.pedido));
}
function renderDeliveryNotice() {
  const cantidad=ownReadyOrders().reduce((sum,c)=>sum+c.pedido.cantidad,0);
  $("entregasBadge").textContent=cantidad;$("entregasBadge").hidden=!cantidad;
  $("avisoEntregas").hidden=!cantidad||state.perfil!=="mozo"||state.view==="entregas";
  $("avisoEntregasTexto").textContent=cantidad+(cantidad===1?" producto listo para entregar":" productos listos para entregar");
}
function renderEntregas() {
  const cola=state.entregasFiltro==="mios"?ownReadyOrders():state.entregas;
  $("entregasTotal").textContent=cola.reduce((sum,c)=>sum+c.pedido.cantidad,0)+" productos pendientes de entrega";
  document.querySelectorAll("[data-entregas-filtro]").forEach(button=>{const active=button.dataset.entregasFiltro===state.entregasFiltro;button.classList.toggle("active",active);button.setAttribute("aria-pressed",String(active));});
  const grupos=groupBy(cola,c=>JSON.stringify([c.pedido.cuentaId,c.pedido.nombreCliente,c.pedido.perfilMozoId]));
  $("listaEntregas").innerHTML=[...grupos.values()].map(items=>{
    const primero=items[0],p=primero.pedido;
    return '<article class="comanda delivery-comanda"><div class="comanda-heading"><div><h3>'+escapeHtml(primero.mesa.nombre)+'</h3><small>Mozo: '+escapeHtml(p.mozo)+(p.nombreCliente?' \u00b7 '+escapeHtml(p.nombreCliente):"")+' \u00b7 Cuenta '+p.cuentaId+'</small></div><span class="badge ready">Listo</span></div>'+items.map(({pedido})=>'<div class="comanda-item" data-entrega-item="'+pedido.id+'"><div class="comanda-item-title"><span class="badge ready">'+pedido.cantidad+'x</span><strong>'+escapeHtml(pedido.nombreProducto)+'</strong></div>'+(pedido.notas?'<p class="notes">'+escapeHtml(pedido.notas)+'</p>':"")+'<button class="success" data-entregar="'+pedido.id+'" '+(!esPedidoPropio(pedido)?'disabled title="Confirma la entrega '+escapeHtml(pedido.mozo)+'"':"")+'>'+icon("check-check")+'Confirmar entrega</button></div>').join("")+'</article>';
  }).join("")||'<p class="empty-column">No hay pedidos listos para entregar</p>';
  renderDeliveryNotice();updateButtons();icons();
}
async function loadEntregas(force=false) {
  const cola=await api("/cocina");state.entregas=cola.filter(c=>c.pedido.estadoCocina==="LISTO");
  const mozo=state.perfilMozoId;
  if(mozo!==state.entregasMozo) {state.entregasMozo=mozo;state.entregasKnown.clear();}
  const propias=ownReadyOrders(),nuevos=propias.filter(c=>!state.entregasKnown.has(c.pedido.id));
  state.entregasKnown=new Set(propias.map(c=>c.pedido.id));
  const signature=JSON.stringify([mozo,state.entregasFiltro,state.entregas]);
  if(force||signature!==state.entregasSignature) {state.entregasSignature=signature;renderEntregas();}
  if(mozo&&nuevos.length&&state.perfil==="mozo") {
    toast(nuevos.map(c=>c.mesa.nombre+": "+c.pedido.cantidad+"x "+c.pedido.nombreProducto).join("; ")+" listo para retirar de cocina");
  }
}
async function loadStatus() {
  state.estado=await api("/estado");
  $("fechaActual").textContent=new Date(state.estado.fecha+"T12:00:00").toLocaleDateString("es-AR",{day:"numeric",month:"long",year:"numeric"});
  $("estadoSistema").textContent=(state.estado.cerrado?"Jornada cerrada":"Jornada abierta")+" / "+(state.estado.baseDatos==="DEMO"?"Demostracion":"MySQL")+" / Comprobantes de prueba";
  if(!$("cierreFecha").value)$("cierreFecha").value=state.estado.fecha;
  updateButtons();
}
function renderRole() {
  $("mozoNav").hidden=state.perfil==="cocina";$("mozoLabel").hidden=state.perfil==="cocina";
  document.querySelectorAll("[data-perfil]").forEach(element=>{element.classList.toggle("active",element.dataset.perfil===state.perfil);element.setAttribute("aria-current",element.dataset.perfil===state.perfil?"page":"false");});
  renderDeliveryNotice();
}
async function changeView(view,updateUrl=true) {
  state.view=view;document.querySelectorAll(".view").forEach(element=>element.classList.toggle("active",element.id==="view-"+view));
  document.querySelectorAll(".nav-button").forEach(element=>element.classList.toggle("active",element.dataset.view===view));
  const paths={mesas:"/mozo",entregas:"/entregas",cocina:"/cocina",menu:"/gestion-menu",tickets:"/historial-tickets",historial:"/historial-pedidos",cierres:"/resumen-diario"};
  if(updateUrl)history.replaceState(null,"",paths[view]);
  if(view==="mesas")await loadMesas(true);
  if(state.perfil==="mozo")await loadEntregas(true);
  renderDeliveryNotice();
  if(view==="cocina")await loadCocina(true);
  if(view==="menu")await loadMenu();
  if(view==="tickets")await loadTickets();
  if(view==="historial")await loadHistorial();
  if(view==="cierres")await loadCierres();
}
async function changeRole(perfil) {
  state.perfil=perfil;renderRole();await changeView(perfil==="cocina"?"cocina":"mesas");
}
document.addEventListener("click",event=>{
  const profile=event.target.closest("[data-perfil]");
  if(profile) {event.preventDefault();action(null,()=>changeRole(profile.dataset.perfil));return;}
  const button=event.target.closest("button");if(!button)return;
  if(button.classList.contains("close-dialog")) {closeDialog(button.closest("dialog"));return;}
  if(button.dataset.view)action(button,()=>changeView(button.dataset.view));
  if(button.dataset.pedidos)action(button,()=>openOrders(Number(button.dataset.pedidos)));
  if(button.dataset.pagar)action(button,()=>openPayment(Number(button.dataset.pagar),button.hasAttribute("data-cliente")?button.dataset.cliente:null));
  if(button.dataset.consumos)action(button,()=>openConsumos(Number(button.dataset.consumos)));
  if(button.hasAttribute("data-categoria-pedido")) {state.categoriaPedido=button.dataset.categoriaPedido;renderCatalog();}
  if(button.dataset.cartEdit) {const item=state.cart.find(p=>p.id===Number(button.dataset.cartEdit));configureProduct(item.productoId,item);}
  if(button.dataset.cartCoffee) {const item=state.cart.find(p=>p.id===Number(button.dataset.cartCoffee));configureProduct(item.productoId,item,"cafe");}
  if(button.dataset.cartRemove) {const item=state.cart.find(p=>p.id===Number(button.dataset.cartRemove));state.cart=state.cart.filter(p=>p.id!==item.id);renderCart();toast(item.nombre+" eliminado de la tanda");}
  if(button.dataset.cartId) {const item=state.cart.find(p=>p.id===Number(button.dataset.cartId));item.cantidad=Math.max(1,Math.min(1000,item.cantidad+Number(button.dataset.cartStep)));renderCart();toast(item.cantidad+"x "+item.nombre+" en la tanda");}
  if(button.hasAttribute("data-cafe-type"))chooseCoffee(button.dataset.cafeType);
  if(button.dataset.cancelar)action(button,async()=>{if(await confirmAction("Cancelar pedido","Cancelar este pedido de la mesa?")) {await post("/pedidos/"+button.dataset.cancelar+"/cancelar");await openConsumos(state.mesaId);await loadMesas(true);toast("Pedido cancelado");}});
  if(button.dataset.cocinaPedido)action(button,async()=>{await api("/pedidos/"+button.dataset.cocinaPedido+"/estado",{method:"PUT",body:JSON.stringify({estado:button.dataset.next})});await loadCocina(true);toast(button.dataset.next==="LISTO"?"Pedido listo para que lo retire el mozo":"Pedido en preparacion");});
  if(button.hasAttribute("data-entregas-filtro")) {state.entregasFiltro=button.dataset.entregasFiltro;renderEntregas();}
  if(button.dataset.entregar)action(button,async()=>{await post("/pedidos/"+button.dataset.entregar+"/entregar",{perfilMozoId:state.perfilMozoId});await loadEntregas(true);await loadMesas(true);toast("Entrega confirmada");});
  if(button.dataset.usarPerfil) {seleccionarPerfil(button.dataset.usarPerfil);$("perfilesDialog").close();toast("Perfil activo: "+state.perfiles.find(p=>p.id===state.perfilMozoId).nombre);}
  if(button.dataset.borrarPerfil)action(button,async()=>{
    const perfil=state.perfiles.find(p=>p.id===Number(button.dataset.borrarPerfil));
    if(!await confirmAction("Eliminar perfil","Eliminar a "+perfil.nombre+" de los perfiles disponibles? Su registro de pedidos se conserva."))return;
    await api("/perfiles/"+perfil.id,{method:"DELETE"});
    if(state.perfilMozoId===perfil.id)seleccionarPerfil(null);
    await loadPerfiles();toast("Perfil eliminado. El historial se conserva");
  });
  if(button.dataset.editar)openProducto(menuProducts().find(p=>p.id===Number(button.dataset.editar)));
  if(button.dataset.eliminar) {
    const id=Number(button.dataset.eliminar),producto=menuProducts().find(p=>p.id===id);
    if(id<0)state.cambios.delete(id);else state.cambios.set(id,{tipo:"ELIMINAR",producto});
    renderMenu();
  }
  if(button.dataset.restaurar) {state.cambios.delete(Number(button.dataset.restaurar));renderMenu();}
  if(button.dataset.ticket)action(button,()=>openTicket(Number(button.dataset.ticket)));
  if(button.dataset.cierre)action(button,()=>openCierre(button.dataset.cierre));
});
$("confirmYes").addEventListener("click",()=>resolveConfirm(true));
$("confirmNo").addEventListener("click",()=>resolveConfirm(false));
$("confirmDialog").addEventListener("cancel",event=>{event.preventDefault();resolveConfirm(false);});
$("pedidosDialog").addEventListener("cancel",event=>{event.preventDefault();closeDialog($("pedidosDialog"));});
$("mozo").addEventListener("change",event=>seleccionarPerfil(event.target.value));
$("gestionarPerfiles").addEventListener("click",event=>action(event.currentTarget,openPerfiles));
$("perfilForm").addEventListener("submit",event=>{event.preventDefault();action(event.submitter,async()=>{
  const perfil=await post("/perfiles",{nombre:$("perfilNombre").value});
  await loadPerfiles();seleccionarPerfil(perfil.id);$("perfilesDialog").close();toast("Perfil guardado: "+perfil.nombre);
});});
$("historialFecha").addEventListener("change",()=>action(null,loadHistorial));
$("historialMozo").addEventListener("change",()=>action(null,loadHistorial));
$("cocinaBuscar").addEventListener("input",renderCocina);
$("buscarPlato").addEventListener("input",renderCatalog);
$("listaMenu").addEventListener("click",event=>{
  const button=event.target.closest("button[data-producto]");
  if(!button)return;
  event.stopPropagation();
  selectProduct(Number(button.dataset.producto));
});
$("clienteForm").addEventListener("submit",event=>{event.preventDefault();$("pedidoCliente").value=$("clienteNombre").value.trim();$("clienteDialog").close();showOrders();});
$("detalleProductoForm").addEventListener("submit",event=>{event.preventDefault();try {addConfiguredProduct();}catch(error){toast(error.message,true);}});
$("pedidosPendientesContainer").addEventListener("change",event=>{if(event.target.dataset.cartQuantity) {const item=state.cart.find(p=>p.id===Number(event.target.dataset.cartQuantity));item.cantidad=Math.max(1,Math.min(1000,Math.trunc(Number(event.target.value)||1)));renderCart();toast(item.cantidad+"x "+item.nombre+" en la tanda");}});
$("toastClose").addEventListener("click",hideToast);
$("enviarCocina").addEventListener("click",event=>action(event.currentTarget,sendComanda));
$("enviarBorradores").addEventListener("click",event=>action(event.currentTarget,async()=>{
  await post("/cuentas/"+state.cuenta.id+"/cocina",{pedidosIds:state.cuenta.pedidos.filter(p=>!p.cancelado&&p.estadoCocina==="BORRADOR").map(p=>p.id)});
  await openConsumos(state.mesaId);await loadMesas(true);
}));
$("liberarMesa").addEventListener("click",event=>action(event.currentTarget,async()=>{await post("/cuentas/"+state.cuenta.id+"/cerrar");$("consumosDialog").close();await loadMesas(true);toast("Mesa liberada");}));
$("pagoItems").addEventListener("input",event=>{
  const input=event.target;if(!input.dataset.pago)return;
  const cantidad=Math.max(0,Math.min(Number(input.max),Math.trunc(Number(input.value)||0)));
  input.value=cantidad;state.seleccion.set(Number(input.dataset.pago),cantidad);
  $("pagoItems").querySelector('[data-pay-check="'+input.dataset.pago+'"]').checked=cantidad>0;updatePayment();
});
$("pagoItems").addEventListener("change",event=>{
  const check=event.target;if(!check.dataset.payCheck)return;
  const id=Number(check.dataset.payCheck),pedido=state.pagoPedidos.find(p=>p.id===id),cantidad=check.checked?remaining(pedido):0;
  state.seleccion.set(id,cantidad);$("pagoItems").querySelector('[data-pago="'+id+'"]').value=cantidad;updatePayment();
});
$("seleccionarTodos").addEventListener("change",event=>{
  for(const p of state.pagoPedidos)state.seleccion.set(p.id,event.target.checked?remaining(p):0);
  document.querySelectorAll(".pay-check").forEach(input=>input.checked=event.target.checked);
  document.querySelectorAll(".pay-quantity").forEach(input=>input.value=state.seleccion.get(Number(input.dataset.pago)));
  updatePayment();
});
$("pagoForm").addEventListener("input",updatePayment);
$("pagoForm").addEventListener("submit",event=>{event.preventDefault();action(event.submitter,payCuenta);});
$("nuevoProducto").addEventListener("click",()=>openProducto());
$("productoForm").addEventListener("submit",event=>{event.preventDefault();stageProduct();});
$("menuCategoria").addEventListener("change",renderMenu);$("menuBuscar").addEventListener("input",renderMenu);
$("menuBody").addEventListener("change",event=>{
  if(event.target.dataset.disponible) {
    const id=Number(event.target.dataset.disponible),producto=menuProducts().find(p=>p.id===id);
    state.cambios.set(id,{tipo:id<0?"CREAR":"ACTUALIZAR",producto:{...producto,disponible:event.target.checked}});
    renderMenu();
  }
});
$("aplicarCambios").addEventListener("click",event=>action(event.currentTarget,applyMenu));
$("descartarCambios").addEventListener("click",()=>{state.cambios.clear();renderMenu();});
$("nuevaCategoria").addEventListener("click",()=>{$("categoriaForm").reset();$("categoriaDialog").showModal();});
$("categoriaForm").addEventListener("submit",event=>{
  event.preventDefault();action(event.submitter,async()=>{await post("/categorias",{nombre:$("categoriaNombre").value});$("categoriaDialog").close();await loadMenu();toast("Categoria guardada");});
});
$("ticketFecha").addEventListener("change",renderTickets);
$("imprimirNavegador").addEventListener("click",()=>window.print());
$("imprimirServidor").addEventListener("click",event=>action(event.currentTarget,async()=>{await post(state.cierreImpresion?"/cierres/imprimir?fecha="+state.cierreImpresion:"/tickets/"+state.ticketId+"/imprimir");toast("Comprobante enviado a la impresora");}));
$("cierreFecha").addEventListener("change",()=>action(null,loadCierres));
$("cerrarDia").addEventListener("click",event=>action(event.currentTarget,async()=>{
  if(await confirmAction("Cerrar jornada","Cerrar la jornada del "+$("cierreFecha").value+"? No se admitiran nuevos pedidos ni pagos para esa fecha.")) {
    await post("/cierres?fecha="+$("cierreFecha").value);await loadStatus();await loadCierres();toast("Cierre registrado");
  }
}));
$("refresh").addEventListener("click",event=>action(event.currentTarget,async()=>{await loadStatus();await changeView(state.view,false);toast("Datos actualizados");}));
window.addEventListener("beforeunload",event=>{if(state.cart.length||state.cambios.size) {event.preventDefault();event.returnValue="";}});
function updateClock() {$("horaActual").textContent=new Date().toLocaleTimeString("es-AR",{hour:"2-digit",minute:"2-digit"});}
async function poll() {
  if(document.hidden||state.busy||state.polling)return;
  state.polling=true;
  try {await loadStatus();if(state.perfil==="mozo") {await loadPerfiles();await loadEntregas();}if(!document.querySelector("dialog[open]")) {if(state.perfil==="cocina")await loadCocina();else if(state.view==="mesas")await loadMesas();}}
  catch {$("estadoSistema").textContent="Sin conexion";}
  finally {state.polling=false;}
}
async function start() {
  icons();renderRole();updateClock();
  try {await loadStatus();await loadPerfiles();await loadMenu();await loadMesas();await changeView(state.view,false);if(state.perfil==="mozo"&&!state.perfilMozoId)await openPerfiles();}
  catch(error) {$("estadoSistema").textContent="Sin conexion";toast(error.message,true);}
  setInterval(updateClock,1000);setInterval(poll,3000);
}
start();
