// =========================================================
// USER DASHBOARD
// =========================================================

let currentUser = null;
let myOrders = [];
let myTickets = [];


// =========================================================
// HELPERS
// =========================================================

function escapeHtml(value) {

    const div =
        document.createElement("div");

    div.textContent =
        value ?? "";

    return div.innerHTML;
}


function escapeAttribute(value) {

    return escapeHtml(value)
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}


function formatDate(value) {

    if (!value) {
        return "—";
    }

    try {

        return new Date(
            value + "T00:00:00"
        ).toLocaleDateString(
            undefined,
            {
                day: "2-digit",
                month: "short",
                year: "numeric"
            }
        );

    } catch (error) {

        return value;
    }
}


function formatDateTime(value) {

    if (!value) {
        return "";
    }

    try {

        return new Date(
            value
        ).toLocaleString();

    } catch (error) {

        return value;
    }
}


function formatAmount(value) {

    const amount =
        Number(value);

    if (!Number.isFinite(amount)) {
        return "₹0.00";
    }

    return new Intl.NumberFormat(
        "en-IN",
        {
            style: "currency",
            currency: "INR"
        }
    ).format(amount);
}


function getPriorityClass(priority) {

    return String(
        priority || "LOW"
    ).toLowerCase();
}


function getStatusClass(status) {

    return String(
        status || "OPEN"
    )
        .toLowerCase()
        .replace("_", "-");
}


// =========================================================
// LOAD CURRENT USER
// =========================================================

async function loadCurrentUser() {

    try {

        const response =
            await fetch(
                "/api/auth/me"
            );

        if (!response.ok) {

            window.location.href =
                "/login.html";

            return;
        }

        currentUser =
            await response.json();

        const customerName =
            document.getElementById(
                "customerName"
            );

        const customerId =
            document.getElementById(
                "customerId"
            );

        if (customerName) {

            customerName.textContent =
                currentUser.name ||
                "Customer";
        }

        if (customerId) {

            customerId.textContent =
                currentUser.customerId ||
                "";
        }

    } catch (error) {

        console.error(
            "User loading error:",
            error
        );

        window.location.href =
            "/login.html";
    }
}


// =========================================================
// LOAD ORDERS
// =========================================================

async function loadOrders() {

    try {

        const response =
            await fetch(
                "/api/orders/my"
            );

        if (!response.ok) {

            throw new Error(
                "Unable to load orders."
            );
        }

        myOrders =
            await response.json();

        renderOrderDropdown();

        renderOrders();

    } catch (error) {

        console.error(
            "Order loading error:",
            error
        );

        const ordersContainer =
            document.getElementById(
                "ordersList"
            );

        if (ordersContainer) {

            ordersContainer.innerHTML = `
                <div class="empty-state">
                    Unable to load your orders.
                    Please refresh the page.
                </div>
            `;
        }
    }
}


// =========================================================
// ORDER DROPDOWN
// =========================================================

function renderOrderDropdown() {

    const select =
        document.getElementById(
            "ticketOrder"
        );

    if (!select) {
        return;
    }

    select.innerHTML = `
        <option value="">
            Select an order
        </option>
    `;

    myOrders.forEach(order => {

        const option =
            document.createElement(
                "option"
            );

        option.value =
            order.orderId;

        option.textContent =
            `${order.orderId} — ${
                order.productName
            }`;

        select.appendChild(
            option
        );
    });
}


// =========================================================
// RENDER ORDERS
// =========================================================

function renderOrders() {

    const container =
        document.getElementById(
            "ordersList"
        );

    if (!container) {
        return;
    }

    if (!myOrders.length) {

        container.innerHTML = `
            <div class="empty-state">
                <h3>No orders yet</h3>

                <p>
                    Add your first order above
                    to report an issue against it.
                </p>
            </div>
        `;

        return;
    }

    container.innerHTML =
        myOrders.map(order => {

            return `
                <div class="order-card">

                    <div class="order-card-header">

                        <div>

                            <span class="order-label">
                                ORDER
                            </span>

                            <h3>
                                ${escapeHtml(
                                    order.orderId
                                )}
                            </h3>

                        </div>

                        <span class="order-status">
                            ${escapeHtml(
                                order.status ||
                                "PROCESSING"
                            )}
                        </span>

                    </div>


                    <div class="order-info">

                        <div>

                            <span>
                                Product
                            </span>

                            <strong>
                                ${escapeHtml(
                                    order.productName
                                )}
                            </strong>

                        </div>


                        <div>

                            <span>
                                Amount
                            </span>

                            <strong>
                                ${formatAmount(
                                    order.amount
                                )}
                            </strong>

                        </div>


                        <div>

                            <span>
                                Order Date
                            </span>

                            <strong>
                                ${formatDate(
                                    order.orderDate
                                )}
                            </strong>

                        </div>


                        <div>

                            <span>
                                Delivery Date
                            </span>

                            <strong>
                                ${formatDate(
                                    order.deliveryDate
                                )}
                            </strong>

                        </div>

                    </div>


                    <button
                        type="button"
                        class="report-order-btn"
                        onclick="reportIssueForOrder(
                            '${escapeAttribute(
                                order.orderId
                            )}'
                        )">

                        Report Issue

                    </button>

                </div>
            `;

        }).join("");
}


// =========================================================
// REPORT ISSUE FOR ORDER
// =========================================================

function reportIssueForOrder(
    orderId
) {

    const orderSelect =
        document.getElementById(
            "ticketOrder"
        );

    if (!orderSelect) {
        return;
    }

    orderSelect.value =
        orderId;

    const reportSection =
        document.getElementById(
            "reportIssue"
        );

    if (reportSection) {

        reportSection.scrollIntoView({
            behavior: "smooth"
        });
    }
}


// =========================================================
// LOAD TICKETS
// =========================================================

async function loadTickets() {

    try {

        const response =
            await fetch(
                "/api/tickets"
            );

        if (!response.ok) {

            throw new Error(
                "Unable to load tickets."
            );
        }

        myTickets =
            await response.json();

        renderTickets();

    } catch (error) {

        console.error(
            "Ticket loading error:",
            error
        );

        const container =
            document.getElementById(
                "ticketsList"
            );

        if (container) {

            container.innerHTML = `
                <div class="empty-state">
                    Unable to load support tickets.
                </div>
            `;
        }
    }
}


// =========================================================
// RENDER TICKETS
// =========================================================

function renderTickets() {

    const container =
        document.getElementById(
            "ticketsList"
        );

    if (!container) {
        return;
    }

    if (!myTickets.length) {

        container.innerHTML = `
            <div class="empty-state">

                <h3>
                    No support tickets
                </h3>

                <p>
                    Your submitted complaints
                    will appear here.
                </p>

            </div>
        `;

        return;
    }

    container.innerHTML =
        myTickets.map(ticket => {

            return `

                <div class="ticket-card">

                    <div class="ticket-card-header">

                        <div>

                            <span class="ticket-number">
                                TICKET #${escapeHtml(
                                    ticket.id
                                )}
                            </span>

                            <h3>
                                ${escapeHtml(
                                    ticket.title ||
                                    "Support Request"
                                )}
                            </h3>

                        </div>


                        <span class="
                            ticket-status
                            ${getStatusClass(
                                ticket.status
                            )}
                        ">

                            ${escapeHtml(
                                ticket.status ||
                                "OPEN"
                            )}

                        </span>

                    </div>


                    <p class="ticket-description">

                        ${escapeHtml(
                            ticket.description ||
                            ""
                        )}

                    </p>


                    <div class="ticket-intelligence">

                        <div>

                            <span>
                                Category
                            </span>

                            <strong>
                                ${escapeHtml(
                                    ticket.category ||
                                    "GENERAL"
                                )}
                            </strong>

                        </div>


                        <div>

                            <span>
                                Priority
                            </span>

                            <strong
                                class="
                                    priority-
                                    ${getPriorityClass(
                                        ticket.priority
                                    )}
                                ">

                                ${escapeHtml(
                                    ticket.priority ||
                                    "LOW"
                                )}

                            </strong>

                        </div>


                        <div>

                            <span>
                                Score
                            </span>

                            <strong>
                                ${escapeHtml(
                                    ticket.priorityScore ??
                                    0
                                )}/100
                            </strong>

                        </div>


                        <div>

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

                    </div>


                    ${
                        ticket.order
                            ? `
                                <div class="linked-order">

                                    <strong>
                                        Linked Order
                                    </strong>

                                    <span>
                                        ${escapeHtml(
                                            ticket.order.orderId
                                        )}
                                    </span>

                                    <span>
                                        ${escapeHtml(
                                            ticket.order.productName ||
                                            ""
                                        )}
                                    </span>

                                </div>
                              `
                            : ""
                    }


                    <div class="ai-explanation">

                        <strong>
                            Priority Explanation
                        </strong>

                        <p>
                            ${escapeHtml(
                                ticket.priorityReason ||
                                "No explanation available."
                            )}
                        </p>

                    </div>


                    <div class="ai-response">

                        <strong>
                            AI Suggested Response
                        </strong>

                        <p>
                            ${escapeHtml(
                                ticket.suggestedResponse ||
                                "No response suggestion available."
                            )}
                        </p>

                    </div>


                    <div class="
                        admin-response
                        ${
                            ticket.adminResponse
                                ? "has-response"
                                : "pending"
                        }
                    ">

                        <div class="response-header">

                            <strong>
                                Support Team Response
                            </strong>

                            ${
                                ticket.respondedAt
                                    ? `
                                        <span>
                                            ${escapeHtml(
                                                formatDateTime(
                                                    ticket.respondedAt
                                                )
                                            )}
                                        </span>
                                      `
                                    : ""
                            }

                        </div>


                        <p>

                            ${
                                ticket.adminResponse
                                    ? escapeHtml(
                                        ticket.adminResponse
                                    )
                                    : "Our support team has not responded yet."
                            }

                        </p>

                    </div>

                </div>

            `;

        }).join("");
}


// =========================================================
// CREATE ORDER
// =========================================================

const orderForm =
    document.getElementById(
        "orderForm"
    );

if (orderForm) {

    orderForm.addEventListener(
        "submit",
        async function(event) {

            event.preventDefault();

            const orderId =
                document
                    .getElementById(
                        "orderId"
                    )
                    .value
                    .trim();

            const productName =
                document
                    .getElementById(
                        "productName"
                    )
                    .value
                    .trim();

            const amount =
                document
                    .getElementById(
                        "orderAmount"
                    )
                    .value;

            const orderDate =
                document
                    .getElementById(
                        "orderDate"
                    )
                    .value;

            const deliveryDate =
                document
                    .getElementById(
                        "deliveryDate"
                    )
                    .value;

            const status =
                document
                    .getElementById(
                        "orderStatus"
                    )
                    .value;

            if (
                !orderId ||
                !productName ||
                !amount ||
                !orderDate
            ) {

                alert(
                    "Please complete all required order fields."
                );

                return;
            }

            try {

                const response =
                    await fetch(
                        "/api/orders",
                        {
                            method: "POST",

                            headers: {
                                "Content-Type":
                                    "application/json"
                            },

                            body:
                                JSON.stringify({

                                    orderId,

                                    productName,

                                    amount:
                                        Number(
                                            amount
                                        ),

                                    orderDate,

                                    deliveryDate:
                                        deliveryDate ||
                                        null,

                                    status:
                                        status ||
                                        null
                                })
                        }
                    );

                if (!response.ok) {

                    const errorText =
                        await response.text();

                    throw new Error(
                        errorText ||
                        "Unable to create order."
                    );
                }

                orderForm.reset();

                await loadOrders();

                alert(
                    "Order added successfully."
                );

            } catch (error) {

                console.error(
                    "Create order error:",
                    error
                );

                alert(
                    error.message ||
                    "Unable to create order."
                );
            }
        }
    );
}


// =========================================================
// CREATE TICKET
// =========================================================

const ticketForm =
    document.getElementById(
        "ticketForm"
    );

if (ticketForm) {

    ticketForm.addEventListener(
        "submit",
        async function(event) {

            event.preventDefault();

            const orderId =
                document
                    .getElementById(
                        "ticketOrder"
                    )
                    .value;

            const issueType =
                document
                    .getElementById(
                        "issueType"
                    )
                    .value;

            const title =
                document
                    .getElementById(
                        "ticketTitle"
                    )
                    .value
                    .trim();

            const description =
                document
                    .getElementById(
                        "ticketDescription"
                    )
                    .value
                    .trim();

            if (!issueType ||
                !title ||
                !description) {

                alert(
                    "Please complete the issue type, title and description."
                );

                return;
            }

            const submitButton =
                ticketForm.querySelector(
                    "button[type='submit']"
                );

            const originalText =
                submitButton
                    ? submitButton.textContent
                    : "Submit Ticket";

            if (submitButton) {

                submitButton.disabled =
                    true;

                submitButton.textContent =
                    "Analyzing...";
            }

            try {

                const body = {

                    issueType,

                    title,

                    description
                };

                if (orderId) {

                    body.order = {
                        orderId
                    };
                }

                const response =
                    await fetch(
                        "/api/tickets",
                        {
                            method: "POST",

                            headers: {
                                "Content-Type":
                                    "application/json"
                            },

                            body:
                                JSON.stringify(body)
                        }
                    );

                if (!response.ok) {

                    const errorText =
                        await response.text();

                    throw new Error(
                        errorText ||
                        "Unable to create ticket."
                    );
                }

                const ticket =
                    await response.json();

                ticketForm.reset();

                await loadTickets();

                await loadOrders();

                showCreatedTicketMessage(
                    ticket
                );

            } catch (error) {

                console.error(
                    "Create ticket error:",
                    error
                );

                alert(
                    error.message ||
                    "Unable to submit ticket."
                );

            } finally {

                if (submitButton) {

                    submitButton.disabled =
                        false;

                    submitButton.textContent =
                        originalText;
                }
            }
        }
    );
}


// =========================================================
// CREATED TICKET MESSAGE
// =========================================================

function showCreatedTicketMessage(
    ticket
) {

    const container =
        document.getElementById(
            "ticketSuccess"
        );

    if (!container) {

        alert(
            `Ticket #${ticket.id} created successfully. Priority: ${ticket.priority}, Score: ${ticket.priorityScore}/100`
        );

        return;
    }

    container.innerHTML = `

        <strong>
            Ticket #${escapeHtml(
                ticket.id
            )} created successfully.
        </strong>

        <p>

            Priority:
            <strong>
                ${escapeHtml(
                    ticket.priority ||
                    "LOW"
                )}
            </strong>

            &nbsp; | &nbsp;

            Score:
            <strong>
                ${escapeHtml(
                    ticket.priorityScore ??
                    0
                )}/100
            </strong>

        </p>

    `;

    container.style.display =
        "block";

    container.scrollIntoView({
        behavior: "smooth"
    });
}


// =========================================================
// LOGOUT
// =========================================================

const logoutButton =
    document.getElementById(
        "logoutButton"
    );

if (logoutButton) {

    logoutButton.addEventListener(
        "click",
        async function() {

            window.location.href =
                "/logout";
        }
    );
}


// =========================================================
// INITIAL LOAD
// =========================================================

document.addEventListener(
    "DOMContentLoaded",
    async function() {

        await loadCurrentUser();

        await Promise.all([
            loadOrders(),
            loadTickets()
        ]);
    }
);