/* =========================================================
   QUEUEBOT ADMIN DASHBOARD
========================================================= */

const API = "";


/* =========================================================
   GLOBAL STATE
========================================================= */

let allTicketsData = [];
let allCustomersData = [];
let allOrdersData = [];
let allProductsData = [];
let currentUser = null;


/* =========================================================
   INITIALIZATION
========================================================= */

document.addEventListener("DOMContentLoaded", async () => {

    setupNavigation();

    setupForms();

    setupFilters();

    setupMobileMenu();

    setupGlobalSearch();

    setCurrentDate();

    await loadCurrentUser();

    await loadDashboard();

});


/* =========================================================
   NAVIGATION
========================================================= */

function setupNavigation() {

    document.querySelectorAll(".nav-item").forEach(item => {

        item.addEventListener("click", event => {

            event.preventDefault();

            const section = item.dataset.section;

            showSection(section);

        });

    });

}


function showSection(section) {

    document.querySelectorAll(".nav-item").forEach(item => {

        item.classList.toggle(
            "active",
            item.dataset.section === section
        );

    });


    document.querySelectorAll(".page-section").forEach(page => {

        page.classList.remove("active-section");

    });


    const target = document.getElementById(
        `${section}-section`
    );

    if (target) {

        target.classList.add("active-section");

    }


    const titles = {

        dashboard: "Dashboard",

        tickets: "Ticket Management",

        customers: "Customers",

        orders: "Orders",

        products: "Products",

        analytics: "Analytics",

        "admin-management": "Admin Management",

        profile: "My Profile"

    };


    document.getElementById("pageTitle").textContent =
        titles[section] || "Dashboard";


    if (section === "tickets") {

        loadTickets();

    }

    if (section === "customers") {

        loadCustomers();

    }

    if (section === "orders") {

        loadOrders();

    }

    if (section === "products") {

        loadProducts();

    }

    if (section === "analytics") {

        loadAnalytics();

    }

    if (section === "profile") {

        renderProfile();

    }


    document
        .getElementById("sidebar")
        ?.classList.remove("open");

}


window.showSection = showSection;


/* =========================================================
   CURRENT USER
========================================================= */

async function loadCurrentUser() {

    try {

        const response = await fetch("/auth/me");

        if (!response.ok) {

            return;

        }

        currentUser = await response.json();

        renderCurrentUser();

    } catch (error) {

        console.warn(
            "Could not load current user:",
            error
        );

    }

}


function renderCurrentUser() {

    if (!currentUser) {

        return;

    }


    const name =
        currentUser.name ||
        currentUser.fullName ||
        "Support Admin";


    const adminId =
        currentUser.adminId ||
        currentUser.customerId ||
        "ADMIN-001";


    const email =
        currentUser.email ||
        "admin@ticketintelligence.com";


    setText(
        "headerAdminName",
        name
    );

    setText(
        "headerAdminId",
        adminId
    );

    setText(
        "welcomeAdminName",
        name.split(" ")[0]
    );

    setText(
        "profileName",
        name
    );

    setText(
        "profileAdminId",
        adminId
    );

    setText(
        "profileEmail",
        email
    );

}


/* =========================================================
   DASHBOARD
========================================================= */

async function loadDashboard() {

    await loadTickets();

    updateDashboardStats();

}


/* =========================================================
   TICKETS
========================================================= */

async function loadTickets() {

    const container =
        document.getElementById("recentTickets");

    if (container) {

        container.innerHTML = `
            <div class="loading-state">
                <div class="spinner"></div>
                <span>Loading tickets...</span>
            </div>
        `;

    }


    try {

        const response = await fetch(
            "/api/tickets"
        );


        if (!response.ok) {

            throw new Error(
                `Ticket request failed: ${response.status}`
            );

        }


        const data = await response.json();

        allTicketsData =
            Array.isArray(data)
                ? data
                : [];


        renderRecentTickets();

        renderPriorityTickets();

        renderAllTickets();

        updateDashboardStats();

        updateAnalytics();


    } catch (error) {

        console.error(error);

        allTicketsData = [];

        renderRecentTickets();

        renderPriorityTickets();

        renderAllTickets();

        updateDashboardStats();

    }

}


/* =========================================================
   RECENT TICKETS
========================================================= */

function renderRecentTickets() {

    const container =
        document.getElementById("recentTickets");

    if (!container) {

        return;

    }


    const tickets =
        [...allTicketsData]
            .sort(
                (a,b) =>
                    new Date(b.createdAt || 0) -
                    new Date(a.createdAt || 0)
            )
            .slice(0,5);


    if (!tickets.length) {

        container.innerHTML = `
            <div class="loading-state">
                No tickets available.
            </div>
        `;

        return;

    }


    container.innerHTML =
        tickets
            .map(ticket => {

                const priority =
                    ticket.finalPriority ||
                    ticket.priority ||
                    "LOW";


                const priorityClass =
                    getPriorityClass(priority);


                return `

                    <div
                        class="ticket-row"
                        onclick="openTicket(${ticket.id})"
                    >

                        <div class="ticket-icon">

                            <i class="fa-solid fa-ticket"></i>

                        </div>


                        <div class="ticket-main">

                            <strong>
                                ${escapeHtml(
                                    ticket.title ||
                                    "Support request"
                                )}
                            </strong>

                            <span>
                                ${escapeHtml(
                                    ticket.ticketId ||
                                    "Ticket"
                                )}
                                •
                                ${escapeHtml(
                                    ticket.category ||
                                    ticket.issueType ||
                                    "GENERAL"
                                )}
                            </span>

                        </div>


                        <div class="ticket-meta">

                            <span class="
                                priority-badge
                                ${priorityClass}
                            ">
                                ${escapeHtml(priority)}
                            </span>

                            <small>
                                ${formatDate(
                                    ticket.createdAt
                                )}
                            </small>

                        </div>

                    </div>

                `;

            })
            .join("");

}


/* =========================================================
   PRIORITY QUEUE
========================================================= */

function renderPriorityTickets() {

    const tbody =
        document.getElementById("priorityTickets");

    if (!tbody) {

        return;

    }


    const tickets =
        [...allTicketsData]
            .sort(
                (a,b) =>
                    (b.priorityScore || 0) -
                    (a.priorityScore || 0)
            )
            .slice(0,8);


    if (!tickets.length) {

        tbody.innerHTML = `
            <tr>
                <td colspan="7" class="empty-cell">
                    No tickets in the queue.
                </td>
            </tr>
        `;

        return;

    }


    tbody.innerHTML =
        tickets
            .map(ticket => {

                const priority =
                    ticket.finalPriority ||
                    ticket.priority ||
                    "LOW";


                return `

                    <tr onclick="openTicket(${ticket.id})"
                        style="cursor:pointer">

                        <td>

                            <span class="ticket-id">
                                ${escapeHtml(
                                    ticket.ticketId ||
                                    `#${ticket.id || ""}`
                                )}
                            </span>

                        </td>


                        <td>
                            ${escapeHtml(
                                getCustomerName(ticket)
                            )}
                        </td>


                        <td>
                            ${escapeHtml(
                                ticket.category ||
                                ticket.issueType ||
                                "GENERAL"
                            )}
                        </td>


                        <td>

                            <span class="
                                sentiment-badge
                                ${getSentimentClass(
                                    ticket.sentiment
                                )}
                            ">

                                ${escapeHtml(
                                    ticket.sentiment ||
                                    "NEUTRAL"
                                )}

                            </span>

                        </td>


                        <td>

                            <span class="
                                priority-badge
                                ${getPriorityClass(priority)}
                            ">

                                ${escapeHtml(priority)}

                            </span>

                        </td>


                        <td>

                            <span class="
                                status-badge
                                ${getStatusClass(
                                    ticket.status
                                )}
                            ">

                                ${escapeHtml(
                                    ticket.status ||
                                    "OPEN"
                                )}

                            </span>

                        </td>


                        <td>

                            <i class="
                                fa-solid
                                fa-chevron-right
                            " style="
                                color:#9aa7b7;
                                font-size:9px;
                            "></i>

                        </td>

                    </tr>

                `;

            })
            .join("");

}


/* =========================================================
   ALL TICKETS
========================================================= */

function renderAllTickets() {

    const tbody =
        document.getElementById("allTickets");

    if (!tbody) {

        return;

    }


    const search =
        (
            document.getElementById(
                "ticketSearch"
            )?.value || ""
        )
        .toLowerCase();


    const priority =
        document.getElementById(
            "priorityFilter"
        )?.value || "";


    const status =
        document.getElementById(
            "statusFilter"
        )?.value || "";


    const filtered =
        allTicketsData.filter(ticket => {

            const text = [

                ticket.ticketId,

                ticket.title,

                ticket.description,

                ticket.category,

                ticket.issueType

            ]
            .join(" ")
            .toLowerCase();


            const currentPriority =
                ticket.finalPriority ||
                ticket.priority ||
                "";


            const currentStatus =
                ticket.status ||
                "";


            return (

                (!search || text.includes(search)) &&

                (!priority ||
                    currentPriority === priority) &&

                (!status ||
                    currentStatus === status)

            );

        });


    if (!filtered.length) {

        tbody.innerHTML = `
            <tr>
                <td colspan="7" class="empty-cell">
                    No tickets found.
                </td>
            </tr>
        `;

        return;

    }


    tbody.innerHTML =
        filtered
            .map(ticket => {

                const p =
                    ticket.finalPriority ||
                    ticket.priority ||
                    "LOW";


                return `

                    <tr
                        onclick="openTicket(${ticket.id})"
                        style="cursor:pointer"
                    >

                        <td>
                            <span class="ticket-id">
                                ${escapeHtml(
                                    ticket.ticketId ||
                                    `#${ticket.id}`
                                )}
                            </span>
                        </td>


                        <td>
                            ${escapeHtml(
                                ticket.title ||
                                ticket.issueType ||
                                "Customer Issue"
                            )}
                        </td>


                        <td>
                            ${escapeHtml(
                                ticket.category ||
                                ticket.issueType ||
                                "GENERAL"
                            )}
                        </td>


                        <td>

                            <span class="
                                priority-badge
                                ${getPriorityClass(p)}
                            ">
                                ${escapeHtml(p)}
                            </span>

                        </td>


                        <td>

                            <span class="
                                sentiment-badge
                                ${getSentimentClass(
                                    ticket.sentiment
                                )}
                            ">
                                ${escapeHtml(
                                    ticket.sentiment ||
                                    "NEUTRAL"
                                )}
                            </span>

                        </td>


                        <td>

                            <span class="
                                status-badge
                                ${getStatusClass(
                                    ticket.status
                                )}
                            ">
                                ${escapeHtml(
                                    ticket.status ||
                                    "OPEN"
                                )}
                            </span>

                        </td>


                        <td>
                            ${formatDate(
                                ticket.createdAt
                            )}
                        </td>

                    </tr>

                `;

            })
            .join("");

}


/* =========================================================
   DASHBOARD STATISTICS
========================================================= */

function updateDashboardStats() {

    const tickets =
        allTicketsData || [];


    const total =
        tickets.length;


    const open =
        tickets.filter(t => {

            const status =
                String(
                    t.status || "OPEN"
                ).toUpperCase();

            return (
                status === "OPEN" ||
                status === "IN_PROGRESS"
            );

        }).length;


    const high =
        tickets.filter(t => {

            const p =
                String(
                    t.finalPriority ||
                    t.priority ||
                    ""
                ).toUpperCase();

            return (
                p === "HIGH" ||
                p === "CRITICAL"
            );

        }).length;


    const critical =
        tickets.filter(t => {

            const p =
                String(
                    t.finalPriority ||
                    t.priority ||
                    ""
                ).toUpperCase();

            return p === "CRITICAL";

        }).length;


    const negative =
        tickets.filter(t => {

            const s =
                String(
                    t.sentiment || ""
                ).toUpperCase();

            return (
                s === "NEGATIVE" ||
                s === "VERY_NEGATIVE"
            );

        }).length;


    const escalation =
        tickets.filter(t => {

            return String(
                t.escalationRisk || ""
            ).toUpperCase() === "HIGH";

        }).length;


    const scores =
        tickets
            .map(t =>
                Number(
                    t.priorityScore
                )
            )
            .filter(
                Number.isFinite
            );


    const average =
        scores.length
            ? (
                scores.reduce(
                    (a,b) => a+b,
                    0
                ) / scores.length
            ).toFixed(1)
            : 0;


    setText("totalTickets", total);

    setText("openTickets", open);

    setText(
        "highPriorityTickets",
        high
    );

    setText(
        "criticalTickets",
        critical
    );

    setText(
        "negativeSentimentTickets",
        negative
    );

    setText(
        "highEscalationRiskTickets",
        escalation
    );

    setText(
        "averagePriorityScore",
        average
    );

    setText(
        "ticketNavCount",
        open
    );


    const bar =
        document.getElementById(
            "priorityBar"
        );

    if (bar) {

        bar.style.width =
            `${Math.min(
                Number(average),
                100
            )}%`;

    }


    setText(
        "analyticsTotal",
        total
    );

    setText(
        "analyticsCritical",
        critical
    );

    setText(
        "analyticsHigh",
        high
    );

    setText(
        "analyticsAverage",
        average
    );


    updateDistribution(
        tickets
    );

}


/* =========================================================
   ANALYTICS
========================================================= */

function updateAnalytics() {

    updateDashboardStats();

}


function loadAnalytics() {

    updateAnalytics();

}


function updateDistribution(tickets) {

    const total =
        tickets.length || 1;


    const counts = {

        critical: tickets.filter(
            t =>
                getPriority(t) ===
                "CRITICAL"
        ).length,

        high: tickets.filter(
            t =>
                getPriority(t) ===
                "HIGH"
        ).length,

        medium: tickets.filter(
            t =>
                getPriority(t) ===
                "MEDIUM"
        ).length,

        low: tickets.filter(
            t =>
                getPriority(t) ===
                "LOW"
        ).length

    };


    setDistribution(
        "critical",
        counts.critical,
        total
    );

    setDistribution(
        "high",
        counts.high,
        total
    );

    setDistribution(
        "medium",
        counts.medium,
        total
    );

    setDistribution(
        "low",
        counts.low,
        total
    );

}


function setDistribution(
    name,
    count,
    total
) {

    const percent =
        Math.round(
            count / total * 100
        );


    const bar =
        document.getElementById(
            `${name}Distribution`
        );


    const text =
        document.getElementById(
            `${name}Percent`
        );


    if (bar) {

        bar.style.width =
            `${percent}%`;

    }


    if (text) {

        text.textContent =
            `${percent}%`;

    }

}


/* =========================================================
   CUSTOMERS
========================================================= */

async function loadCustomers() {

    const tbody =
        document.getElementById(
            "customersTable"
        );


    if (!tbody) {

        return;

    }


    tbody.innerHTML = `
        <tr>
            <td colspan="5"
                class="empty-cell">
                Loading customers...
            </td>
        </tr>
    `;


    try {

        const response =
            await fetch(
                "/api/customers"
            );


        if (!response.ok) {

            throw new Error(
                "Could not load customers"
            );

        }


        const data =
            await response.json();


        allCustomersData =
            Array.isArray(data)
                ? data
                : [];


        renderCustomers();


    } catch (error) {

        console.error(error);

        allCustomersData = [];

        renderCustomers();

    }

}


function renderCustomers() {

    const tbody =
        document.getElementById(
            "customersTable"
        );


    if (!tbody) {

        return;

    }


    setText(
        "customerCount",
        `${allCustomersData.length} customers`
    );


    if (!allCustomersData.length) {

        tbody.innerHTML = `
            <tr>
                <td colspan="5"
                    class="empty-cell">
                    No customers found.
                </td>
            </tr>
        `;

        return;

    }


    tbody.innerHTML =
        allCustomersData
            .map(customer => `

                <tr>

                    <td>
                        <span class="ticket-id">
                            ${escapeHtml(
                                customer.customerId ||
                                "-"
                            )}
                        </span>
                    </td>

                    <td>
                        ${escapeHtml(
                            customer.name ||
                            "-"
                        )}
                    </td>

                    <td>
                        ${escapeHtml(
                            customer.phone ||
                            "-"
                        )}
                    </td>

                    <td>
                        ${escapeHtml(
                            customer.email ||
                            "-"
                        )}
                    </td>

                    <td>

                        <span class="
                            status-badge
                            status-open
                        ">
                            ACTIVE
                        </span>

                    </td>

                </tr>

            `)
            .join("");

}


/* =========================================================
   ORDERS
========================================================= */

async function loadOrders() {

    const tbody =
        document.getElementById(
            "ordersTable"
        );


    if (!tbody) {

        return;

    }


    tbody.innerHTML = `
        <tr>
            <td colspan="7"
                class="empty-cell">
                Loading orders...
            </td>
        </tr>
    `;


    try {

        const response =
            await fetch(
                "/api/orders"
            );


        if (!response.ok) {

            throw new Error(
                "Could not load orders"
            );

        }


        const data =
            await response.json();


        allOrdersData =
            Array.isArray(data)
                ? data
                : [];


        renderOrders();


    } catch (error) {

        console.error(error);

        allOrdersData = [];

        renderOrders();

    }

}


function renderOrders() {

    const tbody =
        document.getElementById(
            "ordersTable"
        );


    if (!tbody) {

        return;

    }


    if (!allOrdersData.length) {

        tbody.innerHTML = `
            <tr>
                <td colspan="7"
                    class="empty-cell">
                    No orders found.
                </td>
            </tr>
        `;

        return;

    }


    tbody.innerHTML =
        allOrdersData
            .map(order => `

                <tr>

                    <td>
                        <span class="ticket-id">
                            ${escapeHtml(
                                order.orderId ||
                                "-"
                            )}
                        </span>
                    </td>

                    <td>
                        ${escapeHtml(
                            order.customer?.name ||
                            order.customerName ||
                            "-"
                        )}
                    </td>

                    <td>
                        ${escapeHtml(
                            order.product?.name ||
                            order.productName ||
                            "-"
                        )}
                    </td>

                    <td>
                        ${escapeHtml(
                            order.quantity ||
                            0
                        )}
                    </td>

                    <td>
                        ₹${formatNumber(
                            order.amount
                        )}
                    </td>

                    <td>
                        ${escapeHtml(
                            order.paymentStatus ||
                            "-"
                        )}
                    </td>

                    <td>

                        <span class="
                            status-badge
                            ${getStatusClass(
                                order.status
                            )}
                        ">
                            ${escapeHtml(
                                order.status ||
                                "PENDING"
                            )}
                        </span>

                    </td>

                </tr>

            `)
            .join("");

}


/* =========================================================
   PRODUCTS
========================================================= */

async function loadProducts() {

    const grid =
        document.getElementById(
            "productsGrid"
        );


    if (!grid) {

        return;

    }


    grid.innerHTML = `
        <div class="loading-state">
            <div class="spinner"></div>
            Loading products...
        </div>
    `;


    try {

        const response =
            await fetch(
                "/api/products"
            );


        if (!response.ok) {

            throw new Error(
                "Could not load products"
            );

        }


        const data =
            await response.json();


        allProductsData =
            Array.isArray(data)
                ? data
                : [];


        renderProducts();


    } catch (error) {

        console.error(error);

        allProductsData = [];

        renderProducts();

    }

}


function renderProducts() {

    const grid =
        document.getElementById(
            "productsGrid"
        );


    if (!grid) {

        return;

    }


    if (!allProductsData.length) {

        grid.innerHTML = `
            <div class="panel"
                 style="padding:40px;grid-column:1/-1">
                <div class="loading-state">
                    No products found.
                </div>
            </div>
        `;

        return;

    }


    grid.innerHTML =
        allProductsData
            .map(product => {

                const image =
                    product.imageUrl;


                return `

                    <div class="product-card">

                        <div class="product-image">

                            ${
                                image

                                ? `
                                    <img
                                        src="${escapeAttribute(
                                            image
                                        )}"
                                        alt="${escapeAttribute(
                                            product.name ||
                                            "Product"
                                        )}"
                                        onerror="
                                            this.style.display='none';
                                            this.parentElement.innerHTML=
                                            '<i class=\\'fa-solid fa-cube product-placeholder\\'></i>';
                                        "
                                    >
                                `

                                : `
                                    <i class="
                                        fa-solid
                                        fa-cube
                                        product-placeholder
                                    "></i>
                                `
                            }

                        </div>


                        <div class="product-body">

                            <span class="panel-kicker">
                                ${escapeHtml(
                                    product.category ||
                                    "PRODUCT"
                                )}
                            </span>

                            <h3>
                                ${escapeHtml(
                                    product.name ||
                                    "Unnamed Product"
                                )}
                            </h3>

                            <div class="product-sku">
                                ${escapeHtml(
                                    product.sku ||
                                    product.productId ||
                                    ""
                                )}
                            </div>


                            <div class="product-info">

                                <strong class="product-price">
                                    ₹${formatNumber(
                                        product.price
                                    )}
                                </strong>

                                <span class="product-stock">
                                    ${product.stockQuantity || 0}
                                    in stock
                                </span>

                            </div>

                        </div>

                    </div>

                `;

            })
            .join("");

}


/* =========================================================
   TICKET DETAILS
========================================================= */

async function openTicket(id) {

    const modal =
        document.getElementById(
            "ticketModal"
        );


    const content =
        document.getElementById(
            "ticketModalContent"
        );


    if (!modal || !content) {

        return;

    }


    modal.classList.add("show");


    content.innerHTML = `
        <div class="loading-state">
            <div class="spinner"></div>
            Loading ticket...
        </div>
    `;


    try {

        const response =
            await fetch(
                `/api/tickets/${id}`
            );


        if (!response.ok) {

            throw new Error(
                "Could not load ticket"
            );

        }


        const ticket =
            await response.json();


        renderTicketDetails(
            ticket
        );


    } catch (error) {

        console.error(error);

        const ticket =
            allTicketsData.find(
                t => String(t.id) === String(id)
            );


        if (ticket) {

            renderTicketDetails(
                ticket
            );

        } else {

            content.innerHTML = `
                <div class="loading-state">
                    Unable to load ticket.
                </div>
            `;

        }

    }

}


window.openTicket = openTicket;


function renderTicketDetails(ticket) {

    const content =
        document.getElementById(
            "ticketModalContent"
        );


    const priority =
        getPriority(ticket);


    content.innerHTML = `

        <div class="ticket-detail-header">

            <span class="ticket-detail-id">
                ${escapeHtml(
                    ticket.ticketId ||
                    `TICKET-${ticket.id}`
                )}
            </span>

            <h2>
                ${escapeHtml(
                    ticket.title ||
                    "Customer Support Issue"
                )}
            </h2>

        </div>


        <div class="ticket-detail-grid">

            <div class="detail-box">

                <span>
                    Category
                </span>

                <strong>
                    ${escapeHtml(
                        ticket.category ||
                        ticket.issueType ||
                        "GENERAL"
                    )}
                </strong>

            </div>


            <div class="detail-box">

                <span>
                    Priority
                </span>

                <strong>

                    <span class="
                        priority-badge
                        ${getPriorityClass(priority)}
                    ">
                        ${escapeHtml(priority)}
                    </span>

                </strong>

            </div>


            <div class="detail-box">

                <span>
                    Sentiment
                </span>

                <strong>
                    ${escapeHtml(
                        ticket.sentiment ||
                        "NEUTRAL"
                    )}
                </strong>

            </div>


            <div class="detail-box">

                <span>
                    Urgency
                </span>

                <strong>
                    ${escapeHtml(
                        ticket.urgency ||
                        "LOW"
                    )}
                </strong>

            </div>


            <div class="detail-box">

                <span>
                    Escalation Risk
                </span>

                <strong>
                    ${escapeHtml(
                        ticket.escalationRisk ||
                        "LOW"
                    )}
                </strong>

            </div>


            <div class="detail-box">

                <span>
                    Priority Score
                </span>

                <strong>
                    ${escapeHtml(
                        ticket.priorityScore ??
                        "0"
                    )}/100
                </strong>

            </div>

        </div>


        <div class="detail-description">

            <h4>
                Customer Description
            </h4>

            <p>
                ${escapeHtml(
                    ticket.description ||
                    "No description provided."
                )}
            </p>

        </div>


        ${
            ticket.priorityReason

            ? `

                <div class="detail-description">

                    <h4>
                        QueueBot Reasoning
                    </h4>

                    <p>
                        ${escapeHtml(
                            ticket.priorityReason
                        )}
                    </p>

                </div>

            `

            : ""
        }


        ${
            ticket.suggestedResponse

            ? `

                <div class="detail-description">

                    <h4>
                        Suggested Response
                    </h4>

                    <p>
                        ${escapeHtml(
                            ticket.suggestedResponse
                        )}
                    </p>

                </div>

            `

            : ""
        }

    `;

}


/* =========================================================
   FORMS
========================================================= */

function setupForms() {

    const customerForm =
        document.getElementById(
            "customerForm"
        );


    if (customerForm) {

        customerForm.addEventListener(
            "submit",
            createCustomer
        );

    }


    const productForm =
        document.getElementById(
            "productForm"
        );


    if (productForm) {

        productForm.addEventListener(
            "submit",
            createProduct
        );

    }


    const adminForm =
        document.getElementById(
            "adminForm"
        );


    if (adminForm) {

        adminForm.addEventListener(
            "submit",
            createAdmin
        );

    }

}


/* =========================================================
   CUSTOMER CREATE
========================================================= */

async function createCustomer(event) {

    event.preventDefault();


    const form =
        event.target;


    const data =
        Object.fromEntries(
            new FormData(form)
        );


    try {

        const response =
            await fetch(
                "/api/customers",
                {
                    method: "POST",
                    headers: {
                        "Content-Type":
                            "application/json"
                    },
                    body: JSON.stringify(data)
                }
            );


        if (!response.ok) {

            const text =
                await response.text();

            throw new Error(
                text || "Customer creation failed"
            );

        }


        closeModal(
            "customerModal"
        );


        form.reset();


        showToast(
            "Customer Created",
            "Customer account created successfully."
        );


        await loadCustomers();


    } catch (error) {

        console.error(error);

        showToast(
            "Creation Failed",
            error.message || "Could not create customer.",
            true
        );

    }

}


/* =========================================================
   PRODUCT CREATE
========================================================= */

async function createProduct(event) {

    event.preventDefault();


    const form =
        event.target;


    const raw =
        Object.fromEntries(
            new FormData(form)
        );


    const data = {

        productId:
            raw.productId,

        sku:
            raw.sku,

        name:
            raw.name,

        category:
            raw.category,

        brand:
            raw.brand,

        description:
            raw.description,

        price:
            Number(raw.price),

        stockQuantity:
            Number(raw.stockQuantity),

        warrantyMonths:
            Number(
                raw.warrantyMonths || 0
            ),

        imageUrl:
            raw.imageUrl,

        status:
            "ACTIVE"

    };


    try {

        const response =
            await fetch(
                "/api/products",
                {
                    method: "POST",
                    headers: {
                        "Content-Type":
                            "application/json"
                    },
                    body: JSON.stringify(data)
                }
            );


        if (!response.ok) {

            const text =
                await response.text();

            throw new Error(
                text || "Product creation failed"
            );

        }


        closeModal(
            "productModal"
        );


        form.reset();


        showToast(
            "Product Added",
            "Product added to your catalog."
        );


        await loadProducts();


    } catch (error) {

        console.error(error);

        showToast(
            "Creation Failed",
            error.message || "Could not create product.",
            true
        );

    }

}


/* =========================================================
   ADMIN CREATE
========================================================= */

async function createAdmin(event) {

    event.preventDefault();


    const form =
        event.target;


    const data =
        Object.fromEntries(
            new FormData(form)
        );


    try {

        const response =
            await fetch(
                "/api/admins",
                {
                    method: "POST",
                    headers: {
                        "Content-Type":
                            "application/json"
                    },
                    body: JSON.stringify(data)
                }
            );


        if (!response.ok) {

            const text =
                await response.text();

            throw new Error(
                text || "Admin creation failed"
            );

        }


        closeModal(
            "adminModal"
        );


        form.reset();


        showToast(
            "Admin Created",
            "Administrator account created successfully."
        );


    } catch (error) {

        console.error(error);

        showToast(
            "Creation Failed",
            error.message || "Could not create administrator.",
            true
        );

    }

}


/* =========================================================
   MODALS
========================================================= */

function openCustomerModal() {

    document
        .getElementById("customerModal")
        ?.classList.add("show");

}


function openProductModal() {

    document
        .getElementById("productModal")
        ?.classList.add("show");

}


function openAdminModal() {

    document
        .getElementById("adminModal")
        ?.classList.add("show");

}


window.openCustomerModal =
    openCustomerModal;

window.openProductModal =
    openProductModal;

window.openAdminModal =
    openAdminModal;


function closeModal(id) {

    document
        .getElementById(id)
        ?.classList.remove("show");

}


window.closeModal =
    closeModal;


document.addEventListener(
    "click",
    event => {

        if (
            event.target.classList.contains(
                "modal-overlay"
            )
        ) {

            event.target.classList.remove(
                "show"
            );

        }

    }
);


/* =========================================================
   FILTERS
========================================================= */

function setupFilters() {

    document
        .getElementById("ticketSearch")
        ?.addEventListener(
            "input",
            renderAllTickets
        );


    document
        .getElementById("priorityFilter")
        ?.addEventListener(
            "change",
            renderAllTickets
        );


    document
        .getElementById("statusFilter")
        ?.addEventListener(
            "change",
            renderAllTickets
        );

}


/* =========================================================
   GLOBAL SEARCH
========================================================= */

function setupGlobalSearch() {

    const input =
        document.getElementById(
            "globalSearch"
        );


    if (!input) {

        return;

    }


    input.addEventListener(
        "keydown",
        event => {

            if (
                event.key === "Enter"
            ) {

                const value =
                    input.value.trim();


                if (!value) {

                    return;

                }


                showSection("tickets");


                const ticketSearch =
                    document.getElementById(
                        "ticketSearch"
                    );


                if (ticketSearch) {

                    ticketSearch.value =
                        value;

                    renderAllTickets();

                }

            }

        }
    );

}


/* =========================================================
   MOBILE MENU
========================================================= */

function setupMobileMenu() {

    document
        .getElementById("mobileMenu")
        ?.addEventListener(
            "click",
            () => {

                document
                    .getElementById("sidebar")
                    ?.classList.toggle("open");

            }
        );

}


/* =========================================================
   PROFILE
========================================================= */

function renderProfile() {

    renderCurrentUser();

}


/* =========================================================
   LOGOUT
========================================================= */

document
    .getElementById("logoutBtn")
    ?.addEventListener(
        "click",
        async () => {

            try {

                await fetch(
                    "/logout",
                    {
                        method: "POST"
                    }
                );

            } catch (error) {

                console.warn(error);

            }


            window.location.href =
                "/login.html";

        }
    );


/* =========================================================
   DATE
========================================================= */

function setCurrentDate() {

    const element =
        document.getElementById(
            "currentDate"
        );


    if (!element) {

        return;

    }


    const now =
        new Date();


    element.textContent =
        now.toLocaleDateString(
            "en-IN",
            {
                weekday: "long",
                day: "numeric",
                month: "long",
                year: "numeric"
            }
        );

}


/* =========================================================
   HELPERS
========================================================= */

function getPriority(ticket) {

    return String(
        ticket.finalPriority ||
        ticket.priority ||
        "LOW"
    ).toUpperCase();

}


function getPriorityClass(priority) {

    switch (
        String(priority)
            .toUpperCase()
    ) {

        case "CRITICAL":
            return "priority-critical";

        case "HIGH":
            return "priority-high";

        case "MEDIUM":
            return "priority-medium";

        default:
            return "priority-low";

    }

}


function getStatusClass(status) {

    const value =
        String(
            status || "OPEN"
        ).toUpperCase();


    if (value === "RESOLVED") {

        return "status-resolved";

    }


    if (
        value === "IN_PROGRESS" ||
        value === "IN PROGRESS"
    ) {

        return "status-progress";

    }


    return "status-open";

}


function getSentimentClass(
    sentiment
) {

    const value =
        String(
            sentiment || "NEUTRAL"
        ).toUpperCase();


    if (
        value === "NEGATIVE" ||
        value === "VERY_NEGATIVE"
    ) {

        return "sentiment-negative";

    }


    if (value === "POSITIVE") {

        return "sentiment-positive";

    }


    return "sentiment-neutral";

}


function getCustomerName(ticket) {

    return (

        ticket.createdBy?.name ||

        ticket.customer?.name ||

        ticket.customerName ||

        "Customer"

    );

}


function formatDate(value) {

    if (!value) {

        return "—";

    }


    try {

        return new Date(
            value
        ).toLocaleDateString(
            "en-IN",
            {
                day: "2-digit",
                month: "short"
            }
        );

    } catch {

        return "—";

    }

}


function formatNumber(value) {

    const number =
        Number(value || 0);


    return number.toLocaleString(
        "en-IN",
        {
            maximumFractionDigits: 2
        }
    );

}


function setText(
    id,
    value
) {

    const element =
        document.getElementById(id);


    if (element) {

        element.textContent =
            value ?? "";

    }

}


function escapeHtml(value) {

    if (value === null ||
        value === undefined) {

        return "";

    }


    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");

}


function escapeAttribute(value) {

    return escapeHtml(value);

}


/* =========================================================
   TOAST
========================================================= */

function showToast(
    title,
    message,
    error = false
) {

    const toast =
        document.getElementById(
            "toast"
        );


    const toastTitle =
        document.getElementById(
            "toastTitle"
        );


    const toastMessage =
        document.getElementById(
            "toastMessage"
        );


    if (!toast) {

        return;

    }


    toastTitle.textContent =
        title;


    toastMessage.textContent =
        message;


    const icon =
        toast.querySelector(
            ".toast-icon"
        );


    if (error) {

        icon.style.background =
            "#fff0f0";

        icon.style.color =
            "#dc2626";

        icon.innerHTML =
            '<i class="fa-solid fa-xmark"></i>';

    } else {

        icon.style.background =
            "#eafaf0";

        icon.style.color =
            "#16a05a";

        icon.innerHTML =
            '<i class="fa-solid fa-check"></i>';

    }


    toast.classList.add(
        "show"
    );


    setTimeout(
        () => {

            toast.classList.remove(
                "show"
            );

        },
        3500
    );

}


window.showToast =
    showToast;